function cargarNotificaciones() {
    console.log("🔄 Cargando notificaciones...");
    
    fetch('/api/notificaciones/no-leidas')
        .then(res => {
            console.log("📡 Status:", res.status);
            return res.json();
        })
        .then(notificaciones => {
            console.log("📩 Notificaciones recibidas:", notificaciones);
            console.log("📩 Cantidad:", notificaciones.length);
            
            const count = notificaciones.length;
            const notifCount = document.getElementById('notifCount');
            const listaNotificaciones = document.getElementById('listaNotificaciones');
            
            if (notifCount) {
                notifCount.textContent = count;
                console.log("🔔 Contador actualizado:", count);
            }
            
            if (listaNotificaciones) {
                if (count === 0) {
                    listaNotificaciones.innerHTML = `
                        <div class="dropdown-item text-muted text-center py-3">
                            <i class="bi bi-check-circle"></i> No hay notificaciones nuevas
                        </div>
                    `;
                } else {
                    let html = '';
                    notificaciones.forEach(n => {
                        let icono = 'bi-info-circle-fill text-primary';
                        let bgClass = '';
                        if (n.tipo === 'SUCCESS') {
                            icono = 'bi-check-circle-fill text-success';
                            bgClass = 'bg-success bg-opacity-10';
                        } else if (n.tipo === 'WARNING') {
                            icono = 'bi-exclamation-triangle-fill text-warning';
                            bgClass = 'bg-warning bg-opacity-10';
                        } else if (n.tipo === 'DANGER') {
                            icono = 'bi-x-circle-fill text-danger';
                            bgClass = 'bg-danger bg-opacity-10';
                        }
                        
                        const fecha = new Date(n.fechaCreacion);
                        const fechaStr = fecha.toLocaleDateString('es-MX', { 
                            day: '2-digit', month: '2-digit', year: 'numeric',
                            hour: '2-digit', minute: '2-digit'
                        });
                        
                        html += `
                            <a class="dropdown-item ${bgClass}" href="${n.urlRelacionada || '#'}" onclick="marcarLeida('${n.id}')">
                                <div class="d-flex align-items-start">
                                    <i class="bi ${icono} me-2 mt-1"></i>
                                    <div>
                                        <strong>${n.titulo}</strong>
                                        <p class="small mb-1 text-muted">${n.mensaje}</p>
                                        <small class="text-muted" style="font-size: 0.65rem;">
                                            <i class="bi bi-clock"></i> ${fechaStr}
                                        </small>
                                    </div>
                                </div>
                            </a>
                        `;
                    });
                    listaNotificaciones.innerHTML = html;
                    console.log("✅ Notificaciones renderizadas:", count);
                }
            }
        })
        .catch(error => {
            console.error('❌ Error cargando notificaciones:', error);
        });
}

function marcarLeida(id) {
    console.log("📌 Marcando como leída:", id);
    fetch('/api/notificaciones/marcar/' + id, { method: 'POST' })
        .then(() => {
            console.log("✅ Marcada como leída:", id);
            cargarNotificaciones();
        })
        .catch(error => console.error('❌ Error:', error));
}

function marcarTodasLeidas() {
    console.log("📌 Marcando todas como leídas");
    fetch('/api/notificaciones/marcar-todas', { method: 'POST' })
        .then(() => {
            console.log("✅ Todas marcadas como leídas");
            cargarNotificaciones();
        })
        .catch(error => console.error('❌ Error:', error));
}

// Cargar al iniciar
document.addEventListener('DOMContentLoaded', function() {
    console.log("🚀 DOM cargado, iniciando notificaciones...");
    cargarNotificaciones();
});

// Recargar cada 30 segundos
setInterval(cargarNotificaciones, 30000);