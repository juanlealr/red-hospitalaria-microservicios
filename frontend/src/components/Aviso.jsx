export default function Aviso({ titulo, texto }) {
    return (
        <>
            <h1>{titulo}</h1>
            {texto && <p className="subtitulo">{texto}</p>}
        </>
    );
}