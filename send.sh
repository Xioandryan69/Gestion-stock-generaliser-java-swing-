#!/bin/bash

# Définir le chemin absolu du script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "=============================================="
echo "   COPIE COMPLÈTE + ARCHIVAGE INTELLIGENT"
echo "=============================================="
echo "Script exécuté depuis : $SCRIPT_DIR"
echo ""

# Options
read -p "Entrez le répertoire source [Entrée = '$SCRIPT_DIR']: " source_dir
read -p "Entrez le répertoire destination [Entrée = '$SCRIPT_DIR/out1']: " dest_dir

# Valeurs par défaut
source_dir="${source_dir:-$SCRIPT_DIR}"
dest_dir="${dest_dir:-$SCRIPT_DIR/out1}"

# Chemins absolus
source_dir=$(realpath -e "$source_dir" 2>/dev/null || echo "$source_dir")
dest_dir=$(realpath -m "$dest_dir")

# Vérification
if [ ! -d "$source_dir" ]; then
    echo "❌ Répertoire source introuvable : $source_dir"
    exit 1
fi

echo ""
echo "📁 Source      : $source_dir"
echo "📁 Destination : $dest_dir"
echo "📄 Copie       : Tous les fichiers sauf *.jar"
echo "📝 Archive     : sources_structured.txt"
echo "🚫 Exclusions  : .git/ et out/*.class"
echo ""

read -p "Continuer ? (o/n) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Oo]$ ]]; then
    echo "❌ Annulé."
    exit 0
fi

# Création destination
mkdir -p "$dest_dir"

# Fichier archive
ARCHIVE_FILE="$SCRIPT_DIR/sources_structured.txt"
TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")

cat > "$ARCHIVE_FILE" <<EOF
################################################################
# ARCHIVE STRUCTURÉE DES FICHIERS
# Générée le: $TIMESTAMP
# Source: $source_dir
# Destination: $dest_dir
################################################################

EOF

# Fonction archive (inchangée)
format_file_for_archive() {
    local file="$1"
    local rel_path="$2"

    local filename=$(basename "$file")
    local extension="${filename##*.}"

    echo "╔══════════════════════════════════════════════════════════╗" >> "$ARCHIVE_FILE"
    echo "║ FICHIER: $rel_path/$filename" >> "$ARCHIVE_FILE"
    echo "║ Chemin : $file" >> "$ARCHIVE_FILE"
    echo "║ Taille : $(stat -c%s "$file" 2>/dev/null) octets" >> "$ARCHIVE_FILE"
    echo "║ Date   : $(stat -c%y "$file" 2>/dev/null | cut -d'.' -f1)" >> "$ARCHIVE_FILE"
    echo "╚══════════════════════════════════════════════════════════╝" >> "$ARCHIVE_FILE"
    echo "" >> "$ARCHIVE_FILE"

    # CAS .jar
    if [[ "$extension" == "jar" ]]; then
        echo "[FICHIER JAR DÉTECTÉ]" >> "$ARCHIVE_FILE"
        echo "Nom uniquement enregistré." >> "$ARCHIVE_FILE"
        echo "" >> "$ARCHIVE_FILE"
        echo "" >> "$ARCHIVE_FILE"
        return
    fi

    # Fichier texte
    if file "$file" | grep -qi "text"; then

        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" >> "$ARCHIVE_FILE"
        echo "CONTENU :" >> "$ARCHIVE_FILE"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" >> "$ARCHIVE_FILE"
        echo "" >> "$ARCHIVE_FILE"

        cat -n "$file" >> "$ARCHIVE_FILE" 2>/dev/null \
        || echo "[Impossible de lire le contenu]" >> "$ARCHIVE_FILE"

        echo "" >> "$ARCHIVE_FILE"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" >> "$ARCHIVE_FILE"

    else
        echo "[FICHIER BINAIRE - contenu ignoré]" >> "$ARCHIVE_FILE"
    fi

    echo "" >> "$ARCHIVE_FILE"
    echo "" >> "$ARCHIVE_FILE"
}

echo ""
echo "🔄 Traitement des fichiers..."
echo ""

# Compteurs
total_files=0
copied_count=0
jar_skipped=0
archived_count=0
error_count=0
ignored_git_out=0

# ======================================================
# PATTERNS D'EXCLUSION (expressions régulières bash)
# ======================================================
exclude_patterns=(
    ".*/\.git/.*"           # tout ce qui est sous .git/
    ".*/out/.*\.class$"     # les .class sous out/ (et ses sous-dossiers)
)

while IFS= read -r file; do

    ((total_files++))

    # Vérifier si le fichier correspond à un motif exclu
    excluded=0
    for pattern in "${exclude_patterns[@]}"; do
        if [[ "$file" =~ $pattern ]]; then
            excluded=1
            break
        fi
    done

    if [ $excluded -eq 1 ]; then
        ((ignored_git_out++))
        continue   # ni copie, ni archivage
    fi

    filename=$(basename "$file")
    extension="${filename##*.}"

    # chemin relatif
    rel_path=$(dirname "${file#$source_dir/}")

    if [ "$rel_path" = "." ]; then
        rel_path=""
    fi

    mkdir -p "$dest_dir/$rel_path"

    # ===============================
    # CAS .jar
    # ===============================
    if [[ "$extension" == "jar" ]]; then

        echo "  🚫 JAR ignoré : $filename"

        format_file_for_archive "$file" "$rel_path"

        ((jar_skipped++))
        ((archived_count++))

        continue
    fi

    # ===============================
    # COPIE NORMALE
    # ===============================
    if cp "$file" "$dest_dir/$rel_path/" 2>/dev/null; then

        echo "  ✓ Copié : $filename"

        ((copied_count++))

        format_file_for_archive "$file" "$rel_path"

        ((archived_count++))

    else

        echo "  ✗ Erreur : $filename"

        ((error_count++))

    fi

done < <(find "$source_dir" -type f 2>/dev/null | sort)

echo ""
echo "=============================================="
echo "RÉSUMÉ"
echo "=============================================="

echo "📊 Total fichiers trouvés    : $total_files"
echo "✅ Fichiers copiés           : $copied_count"
echo "🚫 JAR ignorés               : $jar_skipped"
echo "🙈 Exclus (.git + out/*.class) : $ignored_git_out"
echo "📝 Fichiers archivés         : $archived_count"
echo "❌ Erreurs                   : $error_count"

echo ""
echo "📁 Destination : $dest_dir"
echo "📄 Archive     : $ARCHIVE_FILE"

echo ""
echo "📋 Statistiques extensions"
echo "--------------------------------------------"

declare -A ext_count

while IFS= read -r file; do

    ext="${file##*.}"

    if [ "$ext" = "$file" ]; then
        ext="sans-extension"
    fi

    ((ext_count[$ext]++))

done < <(find "$source_dir" -type f)

printf "%-15s | %s\n" "Extension" "Nombre"
printf "%-15s-+-%s\n" "---------------" "--------"

for ext in "${!ext_count[@]}"; do
    printf "%-15s | %d\n" "$ext" "${ext_count[$ext]}"
done | sort

echo ""
echo "🎉 Terminé."
echo ""

# Aperçu
if [ -f "$ARCHIVE_FILE" ]; then

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📖 Aperçu archive"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    head -50 "$ARCHIVE_FILE"

    echo "..."
    echo ""

    echo "📏 Taille : $(du -h "$ARCHIVE_FILE" | cut -f1)"
    echo "📄 Lignes : $(wc -l < "$ARCHIVE_FILE")"

fi