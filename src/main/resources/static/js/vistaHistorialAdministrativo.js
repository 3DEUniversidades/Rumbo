document.addEventListener('DOMContentLoaded', async () => {
    const contenido=document.getElementById('contenido-historial-admin');
    const estado=document.getElementById('estado-historial-admin');
    const lista=document.getElementById('lista-historial-admin');
    const origen=document.getElementById('origen-historial-admin');
    const anterior=document.getElementById('anterior-historial-admin');
    const siguiente=document.getElementById('siguiente-historial-admin');
    const actualizar=document.getElementById('actualizar-historial-admin');
    const paginaTexto=document.getElementById('pagina-historial-admin');
    let pagina=0,totalPaginas=0,ocupado=false;
    const acciones={ELIMINAR_CUENTA:'Eliminación de cuenta',ELIMINAR_PUBLICACION:'Eliminación de publicación',ACTUALIZAR_CARRERA:'Actualización de carrera',APROBAR:'Solicitud aprobada',RECHAZAR:'Solicitud rechazada',VALIDAR:'Publicación validada',ELIMINAR:'Publicación retirada por moderación'};
    function botones() {origen.disabled=actualizar.disabled=ocupado;anterior.disabled=ocupado||pagina===0;siguiente.disabled=ocupado||pagina+1>=totalPaginas;}
    function par(dl,nombre,valor) {const dt=document.createElement('dt'),dd=document.createElement('dd');dt.textContent=nombre;dd.textContent=valor==null?'Sin dato registrado':String(valor);dl.append(dt,dd);}
    async function cargar(destino=pagina) {
        if(ocupado)return false;
        ocupado=true;botones();estado.textContent='Consultando historial...';lista.replaceChildren();paginaTexto.textContent='';
        try {
            const respuesta=await fetch('/api/admin/historial?origen='+encodeURIComponent(origen.value)+'&pagina='+destino+'&tamano=20',{credentials:'same-origin',cache:'no-store'});
            if(respuesta.status===401){window.location.replace('/login.html');throw new Error('Inicia sesión nuevamente.');}
            if(!respuesta.ok){const error=await respuesta.json().catch(()=>({}));throw new Error(error.campos?Object.values(error.campos).join(' '):error.error||'No se pudo consultar el historial.');}
            const datos=await respuesta.json();pagina=datos.pagina;totalPaginas=datos.totalPaginas;
            if(pagina>0&&!datos.registros.length){pagina=Math.max(0,totalPaginas-1);estado.textContent='El historial cambió. Pulsa Actualizar.';return false;}
            for(const registro of datos.registros) {
                const tarjeta=document.createElement('article');tarjeta.className='card registro-historial-admin';
                const titulo=document.createElement('h3');titulo.textContent=acciones[registro.accion]||registro.accion||'Acción registrada';
                const dl=document.createElement('dl'),fecha=registro.fecha?new Date(registro.fecha):null;
                par(dl,'Fecha',fecha&&!Number.isNaN(fecha.getTime())?fecha.toLocaleString('es-GT'):registro.fecha);
                par(dl,'Responsable',registro.responsable);par(dl,'Sobre',registro.objetivo);par(dl,'Detalle',registro.detalle);
                if(registro.motivo)par(dl,'Motivo',registro.motivo);
                if(registro.origen==='CARRERAS'){par(dl,'Valor anterior',registro.valorAnterior);par(dl,'Valor nuevo',registro.valorNuevo);}
                tarjeta.append(titulo,dl);lista.append(tarjeta);
            }
            estado.textContent=datos.totalRegistros?datos.totalRegistros+' registros en el área seleccionada.':'No hay registros en el área seleccionada.';
            paginaTexto.textContent=totalPaginas?'Página '+(pagina+1)+' de '+totalPaginas:'';return true;
        }catch(error){estado.textContent=error.message;return false;}finally{ocupado=false;botones();}
    }
    origen.addEventListener('change',()=>cargar(0));actualizar.addEventListener('click',()=>cargar());anterior.addEventListener('click',()=>cargar(pagina-1));siguiente.addEventListener('click',()=>cargar(pagina+1));
    try {const sesion=await obtenerSesion();if(!sesion)return;if(sesion.rol!=='ADMIN'){estado.textContent='Esta consulta requiere una cuenta administradora.';return;}contenido.hidden=false;await cargar();}catch(error){estado.textContent=error.message;}
});
