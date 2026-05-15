package gestion.dao;

import crud.GenericDao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import modele.Produit;

public class ProduitDao implements GenericDao<Produit> {
    private Connection connection;

    public ProduitDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(Produit produit) throws Exception {
        if (produit.getId() == 0) {
            insert(produit);
        } else {
            update(produit);
        }
    }

    @Override
    public void insert(Produit produit) throws Exception {
        String sql = "INSERT INTO produit (nom, description, methode_valorisation, code_interne, actif) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, produit.getNom());
            stmt.setString(2, produit.getDescription());
            stmt.setString(3, produit.getMethodeValorisation());
            stmt.setString(4, produit.getCodeInterne());
            stmt.setBoolean(5, produit.isActif());
            stmt.executeUpdate();
            
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                produit.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public void update(Produit produit) throws Exception {
        String sql = "UPDATE produit SET nom = ?, description = ?, methode_valorisation = ?, " +
                "code_interne = ?, actif = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, produit.getNom());
            stmt.setString(2, produit.getDescription());
            stmt.setString(3, produit.getMethodeValorisation());
            stmt.setString(4, produit.getCodeInterne());
            stmt.setBoolean(5, produit.isActif());
            stmt.setInt(6, produit.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws Exception {
        String sql = "DELETE FROM produit WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public Produit findById(int id) throws Exception {
        String sql = "SELECT * FROM produit WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToProduit(rs);
            }
        }
        return null;
    }

    @Override
    public List<Produit> findAll() throws Exception {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produit WHERE actif = true ORDER BY nom";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                produits.add(mapResultSetToProduit(rs));
            }
        }
        return produits;
    }

    public Produit findByNom(String nom) throws Exception {
        String sql = "SELECT * FROM produit WHERE nom = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, nom);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToProduit(rs);
            }
        }
        return null;
    }

    private Produit mapResultSetToProduit(ResultSet rs) throws Exception {
        Produit produit = new Produit();
        produit.setId(rs.getInt("id"));
        produit.setNom(rs.getString("nom"));
        produit.setDescription(rs.getString("description"));
        produit.setMethodeValorisation(rs.getString("methode_valorisation"));
        produit.setCodeInterne(rs.getString("code_interne"));
        produit.setActif(rs.getBoolean("actif"));
        return produit;
    }
}
