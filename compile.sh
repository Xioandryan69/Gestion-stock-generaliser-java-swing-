javac -cp "lib/*" -d bin $(find src -name "*.java")

java -cp "bin:lib/*" ui.exemple.gestion.StockExempleApp

java -cp "bin:lib/*" ui.example.MouvementStockExempleApp