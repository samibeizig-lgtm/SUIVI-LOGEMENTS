# My Properties

Application Android de suivi des logements gérés par **Nexstay**. Design minimaliste et élégant (palette encre / ivoire / bronze), pensée pour retrouver en un coup d'œil toutes les informations d'un logement.

## Fonctionnalités

- **Liste des logements** avec recherche (nom, adresse, propriétaire) et indicateur de contrat signé.
- **Création / modification / suppression** d'un logement (suppression avec confirmation).
- **Carte interactive** (OpenStreetMap, sans clé API) : chaque logement est géocodé automatiquement à partir de son adresse et affiché par un marqueur ; un appui sur le marqueur puis sur sa bulle ouvre la fiche. Bouton « Ouvrir dans Maps » sur la fiche pour lancer l'itinéraire.
- **Stockage local** (Room / SQLite) : les données restent sur le téléphone.

## Fiche logement — rubriques

| Rubrique | Informations |
|---|---|
| Informations générales | Nom, adresse, étage, nombre de pièces, surface, voyageurs max |
| Équipements | Liste à cocher (lave-linge, lave-vaisselle, four, micro-ondes, TV, climatisation, terrasse, piscine, ascenseur, détecteur de fumée…) |
| Parking | Place de parking (oui/non), numéro de place |
| WiFi & Internet | Opérateur, code contrat, nom du WiFi (SSID), code WiFi, débit, nature (Fibre, ADSL, VDSL, 5G) |
| Accès & clés | Nombre de clés, remise des clés (en personne, keybox, serrure connectée), code keybox, code accès résidence |
| Compteurs | N° compteur eau, électricité, gaz |
| Contrat Nexstay | Date du contrat, taux de commission (HT/TTC), contrat signé (oui/non) |
| Propriétaire | Nom, téléphone, email |
| Notes | Champ libre |

## Stack technique

- Kotlin 2.0 · Jetpack Compose (Material 3) · Navigation Compose
- Room (persistance locale) · KSP
- osmdroid (carte OpenStreetMap, aucune clé API requise)
- `android.location.Geocoder` pour convertir l'adresse en coordonnées (connexion internet requise au moment de l'enregistrement)
- minSdk 26 (Android 8.0) · targetSdk 35

## Compiler

Ouvrir le projet dans **Android Studio** (Ladybug ou plus récent), laisser Gradle se synchroniser, puis *Run*. En ligne de commande :

```bash
./gradlew assembleDebug
# APK généré dans app/build/outputs/apk/debug/
```

## Structure

```
app/src/main/java/com/nexstay/myproperties/
├── MainActivity.kt            # Navigation entre les écrans
├── MyPropertiesApp.kt         # Application (Room + config osmdroid)
├── data/                      # Entité Property, DAO, base Room
└── ui/
    ├── PropertyViewModel.kt   # Logique (CRUD + géocodage)
    ├── components/            # Composants réutilisables (sections, champs…)
    ├── screens/               # Liste, formulaire, fiche détail, carte
    └── theme/                 # Thème minimaliste (encre / ivoire / bronze)
```
