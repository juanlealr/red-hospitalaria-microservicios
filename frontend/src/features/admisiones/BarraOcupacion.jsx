export default function BarraOcupacion({ porcentaje, clase }) {
    return (
        <div className="barra-ocupacion" role="img" aria-label={`${porcentaje}% ocupado`}>
            <div className={`relleno relleno-${clase}`} style={{ width: `${Math.min(porcentaje, 100)}%` }} />
        </div>
    );
}