import { estadoDe } from './catalogo';

export default function BadgeOrden({ orden }) {
    const estado = estadoDe(orden);
    return (
        <span className={`badge ${estado.clase}`} title={estado.ayuda}>
            <span className="badge-punto" />
            {estado.texto}
        </span>
    );
}
