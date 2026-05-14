package main;

import crud.CrudRepository;
import crud.GenericDao;
import javax.swing.*;
import model.Personne;
import ui.DynamicTablePanel;

public class Main {
    public static void main(String[] args) {
        try {
            // Test du formulaire automatique
            JFrame frame = new JFrame("Formulaire dynamique");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            //frame.setContentPane(new DynamicFormPanel(Addresse.class));
            frame.setContentPane(new DynamicTablePanel(Personne.class));
            frame.pack();
            frame.setVisible(true);

            // Test du CRUD
            CrudRepository<Personne> dao = new GenericDao<>(Personne.class);
            Personne p = new Personne();
            p.setId("1");
            p.setNom("Jean");
            dao.save(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}