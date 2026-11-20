import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Jeu de la balle : la raquette suit la souris et fait rebondir les balles.
 * Une balle est "sauvée" la première fois que la raquette la touche.
 * La partie dure 5 minutes ; un clic relance une partie terminée.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame fenetre = new JFrame("Jeu de la balle");
            fenetre.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            fenetre.setContentPane(new PanneauJeu());
            fenetre.pack();
            fenetre.setResizable(false);
            fenetre.setLocationRelativeTo(null);
            fenetre.setVisible(true);
        });
    }
}

class Balle {
    static final int RAYON = 12;

    double x, y;
    double vx, vy;
    boolean sauvee;

    Balle(double x, double y, double vx) {
        this.x = x;
        this.y = y;
        this.vx = vx;
    }
}

class PanneauJeu extends JPanel implements ActionListener {

    static final int LARGEUR = 600;
    static final int HAUTEUR = 500;

    static final double GRAVITE = 0.08;
    static final double VITESSE_REBOND = 7.5;
    static final double VITESSE_X_MAX = 3.0;

    static final int RAQ_LARGEUR = 100;
    static final int RAQ_HAUTEUR = 14;
    static final int RAQ_Y = HAUTEUR - 60;

    static final int DUREE_PARTIE_S = 5 * 60;
    static final int INTERVALLE_BALLE_MS = 10_000;

    private final List<Balle> balles = new ArrayList<>();
    private final Random alea = new Random();
    private final Timer minuteur = new Timer(16, this);

    private double raquetteX = (LARGEUR - RAQ_LARGEUR) / 2.0;
    private int sauvees;
    private int totalGenere;
    private boolean enCours;
    private long debutPartie;
    private long derniereBalle;

    public PanneauJeu() {
        setPreferredSize(new Dimension(LARGEUR, HAUTEUR));
        setBackground(new Color(15, 15, 35));

        MouseAdapter souris = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                deplacerRaquette(e.getX());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                deplacerRaquette(e.getX());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!enCours) {
                    nouvellePartie();
                }
            }
        };
        addMouseListener(souris);
        addMouseMotionListener(souris);

        nouvellePartie();
        minuteur.start();
    }

    private void nouvellePartie() {
        balles.clear();
        sauvees = 0;
        totalGenere = 0;
        enCours = true;
        debutPartie = System.currentTimeMillis();
        creerBalle();
    }

    private void creerBalle() {
        double x = 40 + alea.nextDouble() * (LARGEUR - 80);
        double vx = -1 + alea.nextDouble() * 2;
        balles.add(new Balle(x, Balle.RAYON, vx));
        totalGenere++;
        derniereBalle = System.currentTimeMillis();
    }

    private void deplacerRaquette(int sourisX) {
        raquetteX = sourisX - RAQ_LARGEUR / 2.0;
        raquetteX = Math.max(0, Math.min(LARGEUR - RAQ_LARGEUR, raquetteX));
    }

    private long tempsRestantMs() {
        long ecoule = System.currentTimeMillis() - debutPartie;
        return Math.max(0, DUREE_PARTIE_S * 1000L - ecoule);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (enCours) {
            if (tempsRestantMs() == 0) {
                enCours = false;
            } else {
                if (System.currentTimeMillis() - derniereBalle >= INTERVALLE_BALLE_MS) {
                    creerBalle();
                }
                mettreAJourBalles();
            }
        }
        repaint();
    }

    private void mettreAJourBalles() {
        Iterator<Balle> it = balles.iterator();
        while (it.hasNext()) {
            Balle b = it.next();

            b.vy += GRAVITE;
            b.x += b.vx;
            b.y += b.vy;

            if (b.x - Balle.RAYON < 0) {
                b.x = Balle.RAYON;
                b.vx = -b.vx;
            } else if (b.x + Balle.RAYON > LARGEUR) {
                b.x = LARGEUR - Balle.RAYON;
                b.vx = -b.vx;
            }
            if (b.y - Balle.RAYON < 0) {
                b.y = Balle.RAYON;
                b.vy = Math.abs(b.vy);
            }

            // Collision avec la raquette : seulement quand la balle descend
            double basBalle = b.y + Balle.RAYON;
            double basAvant = basBalle - b.vy;
            boolean toucheEnX = b.x >= raquetteX - Balle.RAYON
                    && b.x <= raquetteX + RAQ_LARGEUR + Balle.RAYON;
            if (b.vy > 0 && toucheEnX && basBalle >= RAQ_Y && basAvant <= RAQ_Y + RAQ_HAUTEUR) {
                b.y = RAQ_Y - Balle.RAYON;
                b.vy = -VITESSE_REBOND;

                // L'endroit de l'impact sur la raquette change la direction horizontale
                double centre = raquetteX + RAQ_LARGEUR / 2.0;
                b.vx += ((b.x - centre) / (RAQ_LARGEUR / 2.0)) * 1.5;
                b.vx = Math.max(-VITESSE_X_MAX, Math.min(VITESSE_X_MAX, b.vx));

                if (!b.sauvee) {
                    b.sauvee = true;
                    sauvees++;
                }
            }

            // Balle perdue : sortie par le bas
            if (b.y - Balle.RAYON > HAUTEUR) {
                it.remove();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(new Color(255, 150, 190));
        for (Balle b : balles) {
            g2.fillOval((int) (b.x - Balle.RAYON), (int) (b.y - Balle.RAYON),
                    Balle.RAYON * 2, Balle.RAYON * 2);
        }

        g2.setColor(new Color(235, 240, 200));
        g2.fillRoundRect((int) raquetteX, RAQ_Y, RAQ_LARGEUR, RAQ_HAUTEUR, 6, 6);

        long restant = tempsRestantMs() / 1000;
        String texte = String.format("Temps restant : %02d:%02d  |  Sauvées : %d / Total généré : %d",
                restant / 60, restant % 60, sauvees, totalGenere);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.drawString(texte, 10, 22);

        if (!enCours) {
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, LARGEUR, HAUTEUR);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 28));
            centrer(g2, "Temps écoulé !", HAUTEUR / 2 - 20);
            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            centrer(g2, "Balles sauvées : " + sauvees + " / " + totalGenere, HAUTEUR / 2 + 15);
            g2.setFont(new Font("Arial", Font.ITALIC, 14));
            centrer(g2, "Cliquez pour rejouer", HAUTEUR / 2 + 50);
        }
    }

    private void centrer(Graphics2D g2, String texte, int y) {
        int largeur = g2.getFontMetrics().stringWidth(texte);
        g2.drawString(texte, (LARGEUR - largeur) / 2, y);
    }
}