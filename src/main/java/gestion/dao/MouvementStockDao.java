package gestion.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import modele.MouvementStock;

public class MouvementStockDao {
    private Connection connection;

    public MouvementStockDao(Connection connection) {
        this.connection = connection;
    }

    public void save(MouvementStock mouvement) throws Exception {
        if (mouvement.getId() == 0) {
            insert(mouvement);
        } else {
            update(mouvement);
        }
    }

    public void insert(MouvementStock mouvement) throws Exception {
        String sql = "INSERT INTO mouvement_stock (id_produit, type_mouvement, quantite, prix_unitaire, " +
                "reference_achat, reference_vente, date_mouvement, statut, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, mouvement.getIdProduit());
            stmt.setString(2, mouvement.getTypeMouvement());
            stmt.setBigDecimal(3, mouvement.getQuantite());
            stmt.setBigDecimal(4, mouvement.getPrixUnitaire());
            stmt.setString(5, mouvement.getReferenceAchat());
            stmt.setString(6, mouvement.getReferenceVente());
            stmt.setDate(7, java.sql.Date.valueOf(mouvement.getDateMouvement()));
            stmt.setString(8, mouvement.getStatut());
            stmt.setString(9, mouvement.getNotes());
            stmt.executeUpdate();
            
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                mouvement.setId(keys.getInt(1));
            }
        }
    }

    public void update(MouvementStock mouvement) throws Exception {
        String sql = "UPDATE mouvement_stock SET id_produit = ?, type_mouvement = ?, quantite = ?, " +
                "prix_unitaire = ?, reference_achat = ?, reference_vente = ?, date_mouvement = ?, " +
                "statut = ?, notes = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, mouvement.getIdProduit());
            stmt.setString(2, mouvement.getTypeMouvement());
            stmt.setBigDecimal(3, mouvement.getQuantite());
            stmt.setBigDecimal(4, mouvement.getPrixUnitaire());
            stmt.setString(5, mouvement.getReferenceAchat());
            stmt.setString(6, mouvement.getReferenceVente());
            stmt.setDate(7, java.sql.Date.valueOf(mouvement.getDateMouvement()));
            stmt.setString(8, mouvement.getStatut());
            stmt.setString(9, mouvement.getNotes());
            stmt.setInt(10, mouvement.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws Exception {
        String sql = "DELETE FROM mouvement_stock WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public MouvementStock findById(int id) throws Exception {
        String sql = "SELECT * FROM mouvement_stock WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToMouvement(rs);
            }
        }
        return null;
    }

    public List<MouvementStock> findAll() throws Exception {
        List<MouvementStock> mouvements = new ArrayList<>();
        String sql = "SELECT * FROM mouvement_stock WHERE statut = 'VALIDÉ' ORDER BY date_mouvement DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                mouvements.add(mapResultSetToMouvement(rs));
            }
        }
        return mouvements;
    }

    public List<MouvementStock> findByProduitId(int idProduit) throws Exception {
        List<MouvementStock> mouvements = new ArrayList<>();
        String sql = "SELECT * FROM mouvement_stock WHERE id_produit = ? AND statut = 'VALIDÉ' " +
                "ORDER BY date_mouvement DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idProduit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                mouvements.add(mapResultSetToMouvement(rs));
            }
        }
        return mouvements;
    }

    public List<MouvementStock> findByTypeMouvement(int idProduit, String typeMouvement) throws Exception {
        List<MouvementStock> mouvements = new ArrayList<>();
        String sql = "SELECT * FROM mouvement_stock WHERE id_produit = ? AND type_mouvement = ? " +
                "AND statut = 'VALIDÉ' ORDER BY date_mouvement ASC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idProduit);
            stmt.setString(2, typeMouvement);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                mouvements.add(mapResultSetToMouvement(rs));
            }
        }
        return mouvements;
    }

    private MouvementStock mapResultSetToMouvement(ResultSet rs) throws Exception {
        MouvementStock mouvement = new MouvementStock();
        mouvement.setId(rs.getInt("id"));
        mouvement.setIdProduit(rs.getInt("id_produit"));
        mouvement.setTypeMouvement(rs.getString("type_mouvement"));
        mouvement.setQuantite(rs.getBigDecimal("quantite"));
        mouvement.setPrixUnitaire(rs.getBigDecimal("prix_unitaire"));
        mouvement.setReferenceAchat(rs.getString("reference_achat"));
        mouvement.setReferenceVente(rs.getString("reference_vente"));
        mouvement.setDateMouvement(rs.getDate("date_mouvement").toLocalDate());
        mouvement.setStatut(rs.getString("statut"));
        mouvement.setNotes(rs.getString("notes"));
        return mouvement;
    }
}
