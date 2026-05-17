#!/bin/bash

# =========================================================
# DEPLOY + ARCHIVAGE INTELLIGENT
# =========================================================

# Définir le chemin absolu du script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "=============================================="
echo "   COPIE COMPLÈTE + ARCHIVAGE INTELLIGENT"
echo "=============================================="
echo "Script exécuté depuis : $SCRIPT_DIR"
echo ""

# =========================================================
# OPTIONS
# =========================================================

read -p "Entrez le répertoire source [Entrée = '$SCRIPT_DIR']: " source_dir
read -p "Entrez le répertoire destination [Entrée = '$SCRIPT_DIR/out1']: " dest_dir

# Valeurs par défaut
source_dir="${source_dir:-$SCRIPT_DIR}"
dest_dir="${dest_dir:-$SCRIPT_DIR/out1}"

# Chemins absolus
source_dir=$(realpath -e "$source_dir" 2>/dev/null || echo "$source_dir")
dest_dir=$(realpath -m "$dest_dir")

# =========================================================
# VÉRIFICATION
# =========================================================

if [ ! -d "$source_dir" ]; then
    echo "❌ Répertoire source introuvable : $source_dir"
    exit 1
fi

echo ""
echo "📁 Source      : $source_dir"
echo "📁 Destination : $dest_dir"
echo "📄 Copie       : Tous les fichiers sauf *.jar"
echo "📝 Archive     : sources_structured.txt"
echo "🚫 Exclusions  :"
echo "    - .git/"
echo "    - *.class"
echo "    - out/"
echo "    - build/"
echo "    - target/"
echo ""

read -p "Continuer ? (o/n) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Oo]$ ]]; then
    echo "❌ Annulé."
    exit 0
fi

# =========================================================
# CRÉATION DOSSIER DESTINATION
# =========================================================

mkdir -p "$dest_dir"

# =========================================================
# ARCHIVE
# =========================================================

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

# =========================================================
# FONCTION ARCHIVAGE
# =========================================================

format_file_for_archive() {

    local file="$1"
    local rel_path="$2"

    local filename=$(basename "$file")
    local extension="${filename##*.}"

    {
        echo "╔══════════════════════════════════════════════════════════╗"
        echo "║ FICHIER: $rel_path/$filename"
        echo "║ Chemin : $file"
        echo "║ Taille : $(stat -c%s "$file" 2>/dev/null) octets"
        echo "║ Date   : $(stat -c%y "$file" 2>/dev/null | cut -d'.' -f1)"
        echo "╚══════════════════════════════════════════════════════════╝"
        echo ""
    } >> "$ARCHIVE_FILE"

    # =====================================================
    # CAS JAR
    # =====================================================

    if [[ "$extension" == "jar" ]]; then

        {
            echo "[FICHIER JAR DÉTECTÉ]"
            echo "Nom uniquement enregistré."
            echo ""
            echo ""
        } >> "$ARCHIVE_FILE"

        return
    fi

    # =====================================================
    # FICHIER TEXTE
    # =====================================================

    if file "$file" | grep -qi "text"; then

        {
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            echo "CONTENU :"
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            echo ""
        } >> "$ARCHIVE_FILE"

        cat -n "$file" >> "$ARCHIVE_FILE" 2>/dev/null \
        || echo "[Impossible de lire le contenu]" >> "$ARCHIVE_FILE"

        {
            echo ""
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            echo ""
            echo ""
        } >> "$ARCHIVE_FILE"

    else

        echo "[FICHIER BINAIRE - contenu ignoré]" >> "$ARCHIVE_FILE"
        echo "" >> "$ARCHIVE_FILE"

    fi
}

echo ""
echo "🔄 Traitement des fichiers..."
echo ""

# =========================================================
# COMPTEURS
# =========================================================

total_files=0
copied_count=0
jar_skipped=0
archived_count=0
error_count=0

# =========================================================
# FIND AVEC EXCLUSIONS
# =========================================================

while IFS= read -r file; do

    ((total_files++))

    filename=$(basename "$file")
    extension="${filename##*.}"

    # chemin relatif
    rel_path=$(dirname "${file#$source_dir/}")

    if [ "$rel_path" = "." ]; then
        rel_path=""
    fi

    mkdir -p "$dest_dir/$rel_path"

    # =====================================================
    # CAS .jar
    # =====================================================

    if [[ "$extension" == "jar" ]]; then

        echo "  🚫 JAR ignoré : $filename"

        format_file_for_archive "$file" "$rel_path"

        ((jar_skipped++))
        ((archived_count++))

        continue
    fi

    # =====================================================
    # COPIE
    # =====================================================

    if cp "$file" "$dest_dir/$rel_path/" 2>/dev/null; then

        echo "  ✓ Copié : $filename"

        ((copied_count++))

        format_file_for_archive "$file" "$rel_path"

        ((archived_count++))

    else

        echo "  ✗ Erreur : $filename"

        ((error_count++))

    fi

done < <(

    find "$source_dir" \
        \( \
            -path "*/.git/*" \
            -o -path "*/out/*" \
            -o -path "*/build/*" \
            -o -path "*/target/*" \
            -o -path "$dest_dir/*" \
            -o -name "*.class" \
        \) -prune \
        -o -type f -print | sort

)

# =========================================================
# RÉSUMÉ
# =========================================================

echo ""
echo "=============================================="
echo "RÉSUMÉ"
echo "=============================================="

echo "📊 Total fichiers traités : $total_files"
echo "✅ Fichiers copiés        : $copied_count"
echo "🚫 JAR ignorés            : $jar_skipped"
echo "📝 Fichiers archivés      : $archived_count"
echo "❌ Erreurs                : $error_count"

echo ""
echo "📁 Destination : $dest_dir"
echo "📄 Archive     : $ARCHIVE_FILE"

# =========================================================
# STATISTIQUES EXTENSIONS
# =========================================================

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

done < <(

    find "$source_dir" \
        \( \
            -path "*/.git/*" \
            -o -path "*/out/*" \
            -o -path "*/build/*" \
            -o -path "*/target/*" \
            -o -path "$dest_dir/*" \
            -o -name "*.class" \
        \) -prune \
        -o -type f -print

)

printf "%-20s | %s\n" "Extension" "Nombre"
printf "%-20s-+-%s\n" "--------------------" "--------"

for ext in "${!ext_count[@]}"; do
    printf "%-20s | %d\n" "$ext" "${ext_count[$ext]}"
done | sort

echo ""
echo "🎉 Terminé."
echo ""

# =========================================================
# APERÇU
# =========================================================

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