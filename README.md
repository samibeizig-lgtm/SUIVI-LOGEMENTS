# My Properties

Application Android de suivi des logements gérés par **Nexstay**. Design minimaliste et élégant (palette encre / ivoire / bronze), pensée pour retrouver en un coup d'œil toutes les informations d'un logement.

## Fonctionnalités

- **Liste des logements** avec recherche (nom, adresse, propriétaire) et indicateur de contrat signé.
- **Création / modification / suppression** d'un logement (suppression avec confirmation).
- **Carte vitrine façon affiche Nexstay** — fond sombre, seuls les gouvernorats contenant des logements sont dessinés, cadrage automatique sur la zone couverte, épingles corail avec le nom du logement affiché à côté ; un appui sur l'épingle ouvre une bulle, puis la fiche. 100 % hors-ligne, dessinée par l'application (aucune connexion ni clé API).
- **Placement manuel** : la position du logement se définit en touchant la carte (rubrique Localisation du formulaire) — pas de géocodage approximatif. Une mini-carte de situation est intégrée à la fiche de chaque logement.
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
- Carte des gouvernorats dessinée en Compose Canvas à partir de contours embarqués (source : geoBoundaries, simplifiés à ~2 000 points) — aucune dépendance cartographique, aucune permission
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
├── MyPropertiesApp.kt         # Application (base Room)
├── data/                      # Entité Property, DAO, base Room, contours des gouvernorats
└── ui/
    ├── PropertyViewModel.kt   # Logique (CRUD)
    ├── components/            # Composants réutilisables (sections, champs…)
    ├── map/                   # Carte de Tunisie dessinée en Canvas (gouvernorats, marqueurs)
    ├── screens/               # Liste, formulaire, fiche détail, carte
    └── theme/                 # Thème minimaliste (encre / ivoire / bronze)
```
