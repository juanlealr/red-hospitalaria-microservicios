export default function BadgeEstado({ estado }) {
    const activa = estado === 'ACTIVA';
    return (
        <span className={`badge ${activa ? 'badge-ok' : 'badge-neutro'}`}>
            <span className="badge-punto" />
            {activa ? 'Activa' : 'Dada de alta'}
        </span>
    );
}