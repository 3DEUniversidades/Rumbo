document.addEventListener('DOMContentLoaded',async()=>{
    const seccion=document.getElementById('seccion-historial-supervisor');
    const lista=document.getElementById('lista-historial-supervisor');
    const estado=document.getElementById('estado-historial-supervisor');
    const tipo=document.getElementById('tipo-historial-supervisor');
    const decision=document.getElementById('decision-historial-supervisor');
    const actualizar=document.getElementById('actualizar-historial-supervisor');
    const anterior=document.getElementById('anterior-historial-supervisor');
    const siguiente=document.getElementById('siguiente-historial-supervisor');
    const paginaTexto=document.getElementById('pagina-historial-supervisor');
    let pagina=0,totalPaginas=0,ocupado=false;
    function botones(){tipo.disabled=decision.disabled=actualizar.disabled=ocupado;anterior.disabled=ocupado||pagina===0;siguiente.disabled=ocupado||pagina+1>=totalPaginas;}
    function par(dl,nombre,valor){const dt=document.createElement('dt'),dd=document.createElement('dd');dt.textContent=nombre;dd.textContent=valor==null||valor===''?'Sin dato registrado':String(valor);dl.append(dt,dd);}
    async function cargar(destino=pagina){
        if(ocupado)return false;
        ocupado=true;botones();estado.textContent='Consultando tus decisiones...';lista.replaceChildren();paginaTexto.textContent='';
        try{
            const consulta=new URLSearchParams({tipo:tipo.value,decision:decision.value,pagina:String(destino),tamano:'20'});
            const respuesta=await fetch('/api/perfil/mis-decisiones?'+consulta,{credentials:'same-origin',cache:'no-store'});
            if(respuesta.status===401){window.location.replace('/login.html');throw new Error('Tu sesión terminó.');}
            if(!respuesta.ok){const error=await respuesta.json().catch(()=>({}));throw new Error(error.campos?Object.values(error.campos).join(' '):error.error||'No se pudo consultar tu historial.');}
            const datos=await respuesta.json();pagina=datos.pagina;totalPaginas=datos.totalPaginas;
            if(pagina>0&&!datos.decisiones.length){pagina=Math.max(0,totalPaginas-1);estado.textContent='El historial cambió. Pulsa Actualizar historial.';return false;}
            for(const registro of datos.decisiones){
                const tarjeta=document.createElement('article');tarjeta.className='decision-supervisor';
                const titulo=document.createElement('h3');const tipoTexto=registro.tipo==='PERFIL'?'Cambio de perfil':'Verificación de estudiante';
                const decisionTexto=registro.decision==='APROBAR'?'Aprobada':registro.decision==='RECHAZAR'?'Rechazada':registro.decision;
                titulo.textContent=tipoTexto+' — '+decisionTexto;
                const dl=document.createElement('dl'),fecha=registro.fecha?new Date(registro.fecha):null;
                par(dl,'Fecha',fecha&&!Number.isNaN(fecha.getTime())?fecha.toLocaleString('es-GT'):registro.fecha);
                par(dl,'Cuenta',registro.nombreUsuario||registro.idUsuario||'Cuenta no disponible');
                par(dl,'Solicitud',registro.idSolicitud);par(dl,'Motivo',registro.motivo);tarjeta.append(titulo,dl);lista.append(tarjeta);
            }
            estado.textContent=datos.totalDecisiones?datos.totalDecisiones+' decisiones con los filtros seleccionados.':'No tienes decisiones con los filtros seleccionados.';
            paginaTexto.textContent=totalPaginas?'Página '+(pagina+1)+' de '+totalPaginas:'';return true;
        }catch(error){estado.textContent=error.message;return false;}finally{ocupado=false;botones();}
    }
    tipo.addEventListener('change',()=>cargar(0));decision.addEventListener('change',()=>cargar(0));actualizar.addEventListener('click',()=>cargar());anterior.addEventListener('click',()=>cargar(pagina-1));siguiente.addEventListener('click',()=>cargar(pagina+1));
    try{const sesion=await obtenerSesion();if(!sesion||sesion.rol!=='SUPERVISOR_DE_USUARIOS')return;seccion.hidden=false;await cargar();}catch(error){estado.textContent=error.message;}
});
