async function listarPacientes() {
try{
    const response = await fetch('http://localhost:8080/paciente');
    const pacientes = await response.json();
    
    const lista = document.getElementById('lista-pacientes');
    lista.innerHTML = '';

    if(pacientes.length === 0){
        lista.innerHTML = '<li>Nenhum paciente cadastrado. </li>';
        return;
    }

    pacientes.forEach(p => {
       const item = document.createElement('li');
       item.textContent = `${p.nome} - CPF: ${p.cpf} - Convênio:  ${p.convenio ?? 'não informado'}`;
       lista.appendChild(item);
    });
    }catch{ (erro) 
        console.error('Erro ao buscar pacientes:', erro)
        document.getElementById('lista-pacientes').innerHTML = '<li> Erro ao carregar pacientes. O back-end está rodando mesmo? </li>';
    }
}
        
listarPacientes();