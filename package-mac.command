#!/bin/zsh
set -e
cd "$(dirname "$0")"

echo "Compilazione progetto..."
mvn clean package

echo
echo "Build completata. Per eseguire l'app usa ./run.command oppure mvn javafx:run"
echo "La creazione di un .app standalone può essere aggiunta successivamente con jpackage."
