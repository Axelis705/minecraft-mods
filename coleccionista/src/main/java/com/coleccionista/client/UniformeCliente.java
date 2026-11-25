package com.coleccionista.client;

import com.axeliscore.plat.Ids;
import com.axeliscore.vestir.client.Prenda;
import com.axeliscore.vestir.client.VestuarioCliente;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Uniforme;
import net.minecraft.resources.ResourceLocation;

/**
 * (Cliente) Cómo se ve el uniforme de las piezas: textura tipo skin (sin cabeza: cada quien conserva su cara), una de seis variantes de
 * manchas según quién lo lleva, más gastado con cada prueba que sobrevive (4 niveles), y su número en la tela del pecho y de la espalda.
 */
public final class UniformeCliente {
    private static final int VARIANTES = 6, NIVELES = 4;
    /** [variante][desgaste]: 0 al despertar; sube cada dos pruebas hasta 3. */
    private static final ResourceLocation[][] ANCHO = new ResourceLocation[VARIANTES][NIVELES], DELGADO = new ResourceLocation[VARIANTES][NIVELES];
    /** Tinta del número (plumón negro un poco corrido). */
    private static final int TINTA = 0xFF26211C;

    static {
        for (int i = 0; i < VARIANTES; i++) {
            for (int n = 0; n < NIVELES; n++) {
                String fin = n == 0 ? "" : "_" + n;
                ANCHO[i][n] = Ids.rl(Coleccionista.MODID, "textures/prenda/uniforme_" + (i + 1) + fin + ".png");
                DELGADO[i][n] = Ids.rl(Coleccionista.MODID, "textures/prenda/uniforme_delgado_" + (i + 1) + fin + ".png");
            }
        }
    }

    private UniformeCliente() {
    }

    public static void registrar() {
        Prenda p = Prenda.piel(ANCHO[0][0]).delgada(DELGADO[0][0]).sin(Prenda.Parte.CABEZA)
                .textura(c -> {
                    int v = Math.floorMod(c.semilla(), VARIANTES);
                    int n = Math.min(NIVELES - 1, (c.datos().getInt("pruebas") + 1) / 2);
                    return c.delgado() ? DELGADO[v][n] : ANCHO[v][n];
                })
                .decorar((c, l) -> {
                    String n = c.datos().getString("numero");
                    // en la caja de música (la parte a ciegas) nadie lleva número: no se sabe quién es quién
                    if (n.isEmpty() || c.primeraPersona() || CajaCliente.anonimo()) {
                        return;
                    }
                    float z = 2F + l.inflado() + 0.03F;
                    // pecho: la tela del lado izquierdo de la pieza; espalda: la tela grande del centro
                    l.texto(Prenda.Parte.CUERPO, n, 2.25F, 3.5F, -z, 0.28F, TINTA, false);
                    l.texto(Prenda.Parte.CUERPO, n, 0F, 3.6F, z, 0.5F, TINTA, true);
                });
        VestuarioCliente.registrar(Uniforme.ID, p);
    }
}
