package br.com.cs.ClinicaMedicaNuse.security;

import br.com.cs.ClinicaMedicaNuse.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** Gera o token JWT do usuário: subject = id, claim "perfil", assinado com HS256. */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final Duration validade;

    public TokenService(JwtEncoder jwtEncoder,
                        @Value("${clinica.jwt.expiracao-minutos:120}") long expiracaoMinutos) {
        this.jwtEncoder = jwtEncoder;
        this.validade = Duration.ofMinutes(expiracaoMinutos);
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("clinica-nise")
                .issuedAt(agora)
                .expiresAt(agora.plus(validade))
                .subject(String.valueOf(usuario.getId()))
                .claim("perfil", usuario.getPerfil().name())
                .claim("nome", usuario.getNome())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** Quantos segundos o token emitido agora ainda vale — o front usa para avisar da expiração. */
    public long validadeEmSegundos() {
        return validade.toSeconds();
    }
}
