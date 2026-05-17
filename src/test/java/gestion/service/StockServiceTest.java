package gestion.service;

import gestion.dao.LigneStockDao;
import gestion.dao.MouvementStockDao;
import gestion.dao.ProduitDao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import modele.MouvementStock;
import modele.Produit;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test suite pour vérifier les calculs FIFO / LIFO / CUMP
 * Exemple : 01/5 → 05/5 avec entries et sortie
 */
public class StockServiceTest {

    private StockService stockService;
    private MockProduitDao produitDao;
    private MockMouvementStockDao mouvementDao;
    private MockLigneStockDao ligneStockDao;

    @Before
    public void setUp() {
        produitDao = new MockProduitDao();
        mouvementDao = new MockMouvementStockDao();
        ligneStockDao = new MockLigneStockDao();
        stockService = new StockService(produitDao, mouvementDao, ligneStockDao);
    }

    /**
     * Setup de l'exemple :
     * - 01/5: 5 @ 100 (ENTREE)
     * - 02/5: 10 @ 110 (ENTREE)
     * - 03/5: 5 @ 95 (ENTREE)
     * - 04/5: 13 (SORTIE)
     */
    private void setupExample() throws Exception {
        // Produit FIFO
        Produit produitFIFO = new Produit("Riz", "Grain", "FIFO", "RIZ-001");
        produitFIFO.setId(1);
        produitDao.save(produitFIFO);

        // Produit LIFO
        Produit produitLIFO = new Produit("Maïs", "Grain", "LIFO", "MAIS-001");
        produitLIFO.setId(2);
        produitDao.save(produitLIFO);

        // Produit CUMP
        Produit produitCUMP = new Produit("Blé", "Grain", "CUMP", "BLE-001");
        produitCUMP.setId(3);
        produitDao.save(produitCUMP);

        // Mouvements FIFO
        addMouvement(1, LocalDate.of(2026, 5, 1), "ENTREE", 5, 100);
        addMouvement(1, LocalDate.of(2026, 5, 2), "ENTREE", 10, 110);
        addMouvement(1, LocalDate.of(2026, 5, 3), "ENTREE", 5, 95);
        addMouvement(1, LocalDate.of(2026, 5, 4), "SORTIE", 13, BigDecimal.ZERO);

        // Mouvements LIFO
        addMouvement(2, LocalDate.of(2026, 5, 1), "ENTREE", 5, 100);
        addMouvement(2, LocalDate.of(2026, 5, 2), "ENTREE", 10, 110);
        addMouvement(2, LocalDate.of(2026, 5, 3), "ENTREE", 5, 95);
        addMouvement(2, LocalDate.of(2026, 5, 4), "SORTIE", 13, BigDecimal.ZERO);

        // Mouvements CUMP
        addMouvement(3, LocalDate.of(2026, 5, 1), "ENTREE", 5, 100);
        addMouvement(3, LocalDate.of(2026, 5, 2), "ENTREE", 10, 110);
        addMouvement(3, LocalDate.of(2026, 5, 3), "ENTREE", 5, 95);
        addMouvement(3, LocalDate.of(2026, 5, 4), "SORTIE", 13, BigDecimal.ZERO);
    }

    private void addMouvement(int idProduit, LocalDate date, String type, int quantite, int pu) {
        MouvementStock m = new MouvementStock();
        m.setIdProduit(idProduit);
        m.setDateMouvement(date);
        m.setTypeMouvement(type);
        m.setQuantite(new BigDecimal(quantite));
        m.setPrixUnitaire(new BigDecimal(pu));
        m.setStatut("VALIDÉ");
        m.setReferenceAchat("REF-" + date);
        m.setReferenceVente("REF-" + date);
        try {
            mouvementDao.save(m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addMouvement(int idProduit, LocalDate date, String type, int quantite, BigDecimal pu) {
        MouvementStock m = new MouvementStock();
        m.setIdProduit(idProduit);
        m.setDateMouvement(date);
        m.setTypeMouvement(type);
        m.setQuantite(new BigDecimal(quantite));
        m.setPrixUnitaire(pu);
        m.setStatut("VALIDÉ");
        m.setReferenceAchat("REF-" + date);
        m.setReferenceVente("REF-" + date);
        try {
            mouvementDao.save(m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== TESTS FIFO ====================

    @Test
    public void testFIFO_01Mai_EtatInitial() throws Exception {
        setupExample();
        StockService.EtatStock etat = stockService.computeSnapshotAtDate(1, LocalDate.of(2026, 5, 1), null);
        assertEquals("Quantité 01/5", new BigDecimal("5"), etat.getQuantite());
        assertEquals("Valeur 01/5", new BigDecimal("500"), etat.getValeurStock());
        assertEquals("CUMP 01/5", new BigDecimal("100.0000"), etat.getCump());
    }

    @Test
    public void testFIFO_02Mai_ApresDeuxiemeEntree() throws Exception {
        setupExample();
        StockService.EtatStock etat = stockService.computeSnapshotAtDate(1, LocalDate.of(2026, 5, 2), null);
        assertEquals("Quantité 02/5", new BigDecimal("15"), etat.getQuantite());
        assertEquals("Valeur 02/5", new BigDecimal("1600"), etat.getValeurStock());
        // CUMP = 1600 / 15 = 106.6666...
        BigDecimal expectedCump = new BigDecimal("1600").divide(new BigDecimal("15"), 4, java.math.RoundingMode.HALF_UP);
        assertEquals("CUMP 02/5", expectedCump, etat.getCump());
    }

    @Test
    public void testFIFO_03Mai_ApresToisemeEntree() throws Exception {
        setupExample();
        StockService.EtatStock etat = stockService.computeSnapshotAtDate(1, LocalDate.of(2026, 5, 3), null);
        assertEquals("Quantité 03/5", new BigDecimal("20"), etat.getQuantite());
        assertEquals("Valeur 03/5", new BigDecimal("2075"), etat.getValeurStock());
        // CUMP = 2075 / 20 = 103.75
        BigDecimal expectedCump = new BigDecimal("2075").divide(new BigDecimal("20"), 4, java.math.RoundingMode.HALF_UP);
        assertEquals("CUMP 03/5", expectedCump, etat.getCump());
    }

    @Test
    public void testFIFO_04Mai_ApresSortie13() throws Exception {
        setupExample();
        StockService.EtatStock etat = stockService.computeSnapshotAtDate(1, LocalDate.of(2026, 5, 4), null);
        // FIFO : 5@100 + 8@110 = 1380, reste = 7
        // Reste : 2@110 + 5@95 = 220 + 475 = 695
        assertEquals("Quantité 04/5 FIFO", new BigDecimal("7"), etat.getQuantite());
        assertEquals("Valeur 04/5 FIFO", new BigDecimal("695"), etat.getValeurStock());
        // CUMP = 695 / 7 = 99.2857... (mais historiquement c'est 103.75)
        // Le CUMP calculé après sortie FIFO est basé sur le stock restant
        BigDecimal expectedCump = new BigDecimal("695").divide(new BigDecimal("7"), 4, java.math.RoundingMode.HALF_UP);
        assertEquals("CUMP restant 04/5 FIFO", expectedCump, etat.getCump());
    }

    // ==================== TESTS LIFO ====================

    @Test
    public void testLIFO_04Mai_ApresSortie13() throws Exception {
        setupExample();
        StockService.EtatStock etat = stockService.computeSnapshotAtDate(2, LocalDate.of(2026, 5, 4), "LIFO");
        // LIFO : 5@95 + 8@110 = 1355, reste = 7
        // Reste : 5@100 + 2@110 = 500 + 220 = 720
        assertEquals("Quantité 04/5 LIFO", new BigDecimal("7"), etat.getQuantite());
        assertEquals("Valeur 04/5 LIFO", new BigDecimal("720"), etat.getValeurStock());
        BigDecimal expectedCump = new BigDecimal("720").divide(new BigDecimal("7"), 4, java.math.RoundingMode.HALF_UP);
        assertEquals("CUMP 04/5 LIFO", expectedCump, etat.getCump());
    }

    // ==================== TESTS CUMP ====================

    @Test
    public void testCUMP_03Mai_AvantSortie() throws Exception {
        setupExample();
        StockService.EtatStock etat = stockService.computeSnapshotAtDate(3, LocalDate.of(2026, 5, 3), "CUMP");
        // Avant sortie : Qté=20, Valeur=2075, CUMP=103.75
        assertEquals("Quantité 03/5 CUMP", new BigDecimal("20"), etat.getQuantite());
        assertEquals("Valeur 03/5 CUMP", new BigDecimal("2075"), etat.getValeurStock());
        BigDecimal expectedCump = new BigDecimal("2075").divide(new BigDecimal("20"), 4, java.math.RoundingMode.HALF_UP);
        assertEquals("CUMP 03/5", expectedCump, etat.getCump());
    }

    @Test
    public void testCUMP_04Mai_ApresSortie13() throws Exception {
        setupExample();
        StockService.EtatStock etat = stockService.computeSnapshotAtDate(3, LocalDate.of(2026, 5, 4), "CUMP");
        // Sortie au CUMP : 13 × 103.75 = 1348.75
        // Reste : 20 - 13 = 7 unités
        // Valeur restante : 2075 - 1348.75 = 726.25
        // CUMP restant : 726.25 / 7 = 103.75 (stabilité du CUMP)
        assertEquals("Quantité 04/5 CUMP", new BigDecimal("7"), etat.getQuantite());
        assertEquals("Valeur 04/5 CUMP", new BigDecimal("726.25"), etat.getValeurStock());
        BigDecimal expectedCump = new BigDecimal("726.25").divide(new BigDecimal("7"), 4, java.math.RoundingMode.HALF_UP);
        assertEquals("CUMP 04/5 après sortie", expectedCump, etat.getCump());
    }

    // ==================== TESTS COMPARATIFS ====================

    @Test
    public void testComparaison_CoutSortie_FIFO_vs_LIFO_vs_CUMP() throws Exception {
        setupExample();

        // Avant sortie
        StockService.EtatStock etatAvant = stockService.computeSnapshotAtDate(1, LocalDate.of(2026, 5, 3), null);
        BigDecimal valeurAvant = etatAvant.getValeurStock(); // 2075

        // FIFO
        StockService.EtatStock etatFIFO = stockService.computeSnapshotAtDate(1, LocalDate.of(2026, 5, 4), "FIFO");
        BigDecimal coutFIFO = valeurAvant.subtract(etatFIFO.getValeurStock()); // 2075 - 695 = 1380
        assertEquals("Coût sortie FIFO", new BigDecimal("1380"), coutFIFO);

        // LIFO
        StockService.EtatStock etatLIFO = stockService.computeSnapshotAtDate(2, LocalDate.of(2026, 5, 4), "LIFO");
        BigDecimal coutLIFO = valeurAvant.subtract(etatLIFO.getValeurStock()); // 2075 - 720 = 1355
        assertEquals("Coût sortie LIFO", new BigDecimal("1355"), coutLIFO);

        // CUMP
        StockService.EtatStock etatCUMP = stockService.computeSnapshotAtDate(3, LocalDate.of(2026, 5, 4), "CUMP");
        BigDecimal coutCUMP = valeurAvant.subtract(etatCUMP.getValeurStock()); // 2075 - 726.25 = 1348.75
        assertEquals("Coût sortie CUMP", new BigDecimal("1348.75"), coutCUMP);

        // Vérifier l'ordre : CUMP < LIFO < FIFO
        assertTrue("CUMP < LIFO", coutCUMP.compareTo(coutLIFO) < 0);
        assertTrue("LIFO < FIFO", coutLIFO.compareTo(coutFIFO) < 0);
        assertTrue("CUMP < FIFO", coutCUMP.compareTo(coutFIFO) < 0);
    }

    // ==================== Mock DAOs ====================

    public static class MockProduitDao extends ProduitDao {
        private List<Produit> produits = new ArrayList<>();

        public MockProduitDao() {
            super(null);
        }

        @Override
        public void save(Produit produit) {
            produits.add(produit);
        }

        @Override
        public Produit findById(int id) {
            return produits.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
        }

        @Override
        public List<Produit> findAll() {
            return new ArrayList<>(produits);
        }
    }

    public static class MockMouvementStockDao extends MouvementStockDao {
        private List<MouvementStock> mouvements = new ArrayList<>();

        public MockMouvementStockDao() {
            super(null);
        }

        @Override
        public void save(MouvementStock mouvement) {
            mouvements.add(mouvement);
        }

        @Override
        public List<MouvementStock> findByProduitIdUntilDate(int idProduit, LocalDate date) {
            List<MouvementStock> result = new ArrayList<>();
            for (MouvementStock m : mouvements) {
                if (m.getIdProduit() == idProduit
                        && (m.getDateMouvement().isBefore(date) || m.getDateMouvement().isEqual(date))
                        && "VALIDÉ".equals(m.getStatut())) {
                    result.add(m);
                }
            }
            result.sort((a, b) -> a.getDateMouvement().compareTo(b.getDateMouvement()));
            return result;
        }

        @Override
        public List<MouvementStock> findAll() {
            return new ArrayList<>(mouvements);
        }
    }

    public static class MockLigneStockDao extends LigneStockDao {
        public MockLigneStockDao() {
            super(null);
        }

        @Override
        public List<modele.LigneStock> findByProduitIdWithRestant(int idProduit) {
            return new ArrayList<>();
        }

        @Override
        public List<modele.LigneStock> findByProduitIdDescending(int idProduit) {
            return new ArrayList<>();
        }
    }
}
