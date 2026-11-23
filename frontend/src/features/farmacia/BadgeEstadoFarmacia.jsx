const ESTADOS = {
    RECIBIDA: ['Recibida', 'badge-alto'],
    COBERTURA_VERIFICADA: ['Cobertura verificada', 'badge-alto'],
    DESPACHADA: ['Despachada', 'badge-ok'],
};

export default function BadgeEstadoFarmacia({ estado }) {
    const [texto, clase] = ESTADOS[estado] ?? [estado, 'badge-neutro'];
    return (
        <span className={`badge ${clase}`}>
            <span className="badge-punto" />
            {texto}
        </span>
    );
}
