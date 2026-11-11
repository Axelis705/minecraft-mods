package com.autoscoleccion.net;

/** Servidor -> cliente: puntaje oficial, caja que salió y las probabilidades que tenía cada caja. */
public record GameOutcomePacket(int session, float score, int box, float o0, float o1, float o2) {
}
