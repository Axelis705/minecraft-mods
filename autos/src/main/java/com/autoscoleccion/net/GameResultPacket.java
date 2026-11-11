package com.autoscoleccion.net;

import com.autoscoleccion.game.SkillGame;

import java.util.List;

/** Cliente -> servidor: las teclas de la partida y cuándo terminó (el servidor calcula el puntaje). */
public record GameResultPacket(int session, int endT, List<SkillGame.Input> inputs) {
}
