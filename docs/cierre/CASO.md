# Caso: Red hospitalaria multi-sede

Una red hospitalaria opera varias sedes (por ejemplo Tunja, Duitama y Sogamoso), cada una con servicios como urgencias, UCI, hospitalización y pediatría. Hoy la información de camas, ingresos y medicación vive aislada en cada sede, lo que obliga a llamar por teléfono para saber dónde hay cupo, y un médico que recibe a un paciente trasladado no ve lo que se hizo en otra sede.

El proyecto construye una plataforma que coordina ese recorrido. El personal de admisiones registra el ingreso y consulta la disponibilidad de camas por sede y servicio, y verifica la cobertura del paciente ante una aseguradora externa. El médico traslada al paciente entre sedes o servicios, solicita exámenes y medicación, consulta el estado consolidado desde cualquier sede y da el alta médica. El farmacéutico recibe y despacha las solicitudes de medicamentos. El administrador gestiona los usuarios y consulta el reporte de ocupación para planear capacidad.

Las reglas del negocio fijan los límites: no se traslada a un paciente a una sede sin cama confirmada, un medicamento no se despacha sin solicitud médica, y el alta se rechaza si hay órdenes de laboratorio pendientes. Además, la caída de un sistema externo (laboratorio o aseguradora) no debe detener la operación clínica interna.

Por eso el caso exige decisiones de arquitectura reales: servicios independientes con su propia base de datos, comunicación por eventos, protección frente a fallos de terceros y un modelo de lectura para la consulta consolidada.
