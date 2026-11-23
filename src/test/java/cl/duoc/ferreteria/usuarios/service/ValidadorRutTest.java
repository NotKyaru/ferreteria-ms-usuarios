package cl.duoc.ferreteria.usuarios.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorRutTest {

    @Test
    void rutValido() {
        assertTrue(ValidadorRut.esValido("12345678-5"));
    }

    @Test
    void digitoVerificadorIncorrecto() {
        assertFalse(ValidadorRut.esValido("12345678-9"));
    }

    @Test
    void conPuntosSeNormalizaYEsValido() {
        assertEquals("12345678-5", ValidadorRut.normalizar("12.345.678-5"));
        assertTrue(ValidadorRut.esValido("12.345.678-5"));
    }

    @Test
    void digitoKEnMinuscula() {
        assertEquals("10000013-K", ValidadorRut.normalizar("10000013-k"));
        assertTrue(ValidadorRut.esValido("10000013-k"));
    }

    @Test
    void losRutDeLosUsuariosDeDemoSonValidos() {
        assertTrue(ValidadorRut.esValido("11111111-1"));
        assertTrue(ValidadorRut.esValido("22222222-2"));
        assertTrue(ValidadorRut.esValido("33333333-3"));
    }

    @Test
    void vacioNuloYFormatoIncorrectoSonInvalidos() {
        assertFalse(ValidadorRut.esValido(""));
        assertFalse(ValidadorRut.esValido(null));
        assertFalse(ValidadorRut.esValido("12345678"));
        assertFalse(ValidadorRut.esValido("abc-5"));
    }
}
