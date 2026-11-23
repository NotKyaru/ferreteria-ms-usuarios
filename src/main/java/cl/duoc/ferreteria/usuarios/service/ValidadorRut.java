package cl.duoc.ferreteria.usuarios.service;

/** Validación de RUT chileno con módulo 11. La misma clase se copia en ms-ventas. */
public final class ValidadorRut {

    private ValidadorRut() { }

    /** "12.345.678-k" → "12345678-K" */
    public static String normalizar(String rut) {
        return rut == null ? null : rut.replace(".", "").replace(" ", "").trim().toUpperCase();
    }

    public static boolean esValido(String rut) {
        String r = normalizar(rut);
        if (r == null || !r.matches("\\d{7,8}-[\\dK]")) {
            return false;
        }
        String cuerpo = r.substring(0, r.length() - 2);
        char dv = r.charAt(r.length() - 1);

        // De derecha a izquierda, multiplicando por 2, 3, 4, 5, 6, 7 y volviendo a 2
        int suma = 0;
        int factor = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += (cuerpo.charAt(i) - '0') * factor;
            factor = (factor == 7) ? 2 : factor + 1;
        }

        int resto = 11 - (suma % 11);
        char esperado = (resto == 11) ? '0' : (resto == 10) ? 'K' : (char) ('0' + resto);
        return dv == esperado;
    }
}
