package gestion.dao;

import crud.GenericDao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import modele.LigneStock;

public class LigneStockDao implements GenericDao<LigneStock> {
    private Connection connection;

    public LigneStockDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(LigneStock ligne) throws Exception {
        if (ligne.getId() == 0) {
            insert(ligne);
        } else {
            update(ligne);
        }
    }

    @Override
    public void insert(LigneStock ligne) throws Exception {
        String sql = "INSERT INTO ligne_stock (id_produit, id_mouvement_entree, quantite_initiale, " +
                "quantite_restante, prix_unitaire, date_entree, date_consommation) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, ligne.getIdProduit());
            stmt.setInt(2, ligne.getIdMouvementEntree());
            stmt.setBigDecimal(3, ligne.getQuantiteInitiale());
            stmt.setBigDecimal(4, ligne.getQuantiteRestante());
            stmt.setBigDecimal(5, ligne.getPrixUnitaire());
            stmt.setDate(6, java.sql.Date.valueOf(ligne.getDateEntree()));
            stmt.setDate(7, ligne.getDateConsommation() != null ? 
                    java.sql.Date.valueOf(ligne.getDateConsommation()) : null);
            stmt.executeUpdate();
            
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                ligne.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public void update(LigneStock ligne) throws Exception {
        String sql = "UPDATE ligne_stock SET quantite_restante = ?, date_consommation = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBigDecimal(1, ligne.getQuantiteRestante());
            stmt.setDate(2, ligne.getDateConsommation() != null ? 
                    java.sql.Date.valueOf(ligne.getDateConsommation()) : null);
            stmt.setInt(3, ligne.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws Exception {
        String sql = "DELETE FROM ligne_stock WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public LigneStock findById(int id) throws Exception {
        String sql = "SELECT * FROM ligne_stock WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToLigneStock(rs);
            }
        }
        return null;
    }

    @Override
    public List<LigneStock> findAll() throws Exception {
        List<LigneStock> lignes = new ArrayList<>();
        String sql = "SELECT * FROM ligne_stock ORDER BY date_entree ASC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lignes.add(mapResultSetToLigneStock(rs));
            }
        }
        return lignes;
    }

    public List<LigneStock> findByProduitIdWithRestant(int idProduit) throws Exception {
        List<LigneStock> lignes = new ArrayList<>();
        String sql = "SELECT * FROM ligne_stock WHERE id_produit = ? AND quantite_restante > 0 " +
                "ORDER BY date_entree ASC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idProduit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lignes.add(mapResultSetToLigneStock(rs));
            }
        }
        return lignes;
    }

    public List<LigneStock> findByProduitIdDescending(int idProduit) throws Exception {
        List<LigneStock> lignes = new ArrayList<>();
        String sql = "SELECT * FROM ligne_stock WHERE id_produit = ? AND quantite_restante > 0 " +
                "ORDER BY date_entree DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idProduit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lignes.add(mapResultSetToLigneStock(rs));
            }
        }
        return lignes;
    }

    private LigneStock mapResultSetToLigneStock(ResultSet rs) throws Exception {
        LigneStock ligne = new LigneStock();
        ligne.setId(rs.getInt("id"));
        ligne.setIdProduit(rs.getInt("id_produit"));
        ligne.setIdMouvementEntree(rs.getInt("id_mouvement_entree"));
        ligne.setQuantiteInitiale(rs.getBigDecimal("quantite_initiale"));
        ligne.setQuantiteRestante(rs.getBigDecimal("quantite_restante"));
        ligne.setPrixUnitaire(rs.getBigDecimal("prix_unitaire"));
        ligne.setDateEntree(rs.getDate("date_entree").toLocalDate());
        java.sql.Date dateConsommation = rs.getDate("date_consommation");
        if (dateConsommation != null) {
            ligne.setDateConsommation(dateConsommation.toLocalDate());
        }
        return ligne;
    }
}
