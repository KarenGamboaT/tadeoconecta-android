# Tadeo Conecta — HTML visual parity lock

Source of visual truth: `Tadeo_Conecta_Prototype_v10_identity_navigation_goal.zip`.

## Scope implemented in this Android build

### Complete authentication module
- Login
- Create institutional account
- Role selection during registration only
- Institutional username domain treatment
- Password / confirm password
- Verification with 6 visual OTP cells
- Password recovery
- Loading / success / error states
- Error library for access/account/network scenarios
- Student vs professor routing after login

### Interior preview after authentication
The interior is intentionally a **visual/navigation preview** at this stage. It reproduces the approved HTML visual system and lets the user switch between the principal visual destinations, without implementing every business action yet.

Student visual destinations:
- Home
- Search
- Favorites
- Profile
- Notifications
- Professor profile preview

Professor visual destinations:
- Home
- Results
- Improve
- Profile
- Notifications
- Goal visual state retained from the approved prototype

## Non-negotiable parity rules
- HTML v10 is canonical. Compose must adapt to the HTML; do not redesign.
- Canvas/background: #F7F8FA
- Action blue: #1565FF
- UTadeo blue: #00558F
- Ink: #0F1E3A
- Muted: #6B7280
- Border: #E4E7EC
- Aqua: #00A9A6
- 14–24dp radii according to HTML component geometry.
- 412x917 Android Compact was the approved design reference.
- Cabito assets are displayed with `ContentScale.Fit`; do not crop, posterize, regenerate or derive new images.
- Current Cabito PNGs remain provisional until the approved institutional vector master replaces them.

## Identity
Student: Karen Gamboa — karen.gamboa@utadeo.edu.co
Professor: José A. Franco C. — josea.francoc@utadeo.edu.co
Professor demo login: josea.francoc / prueba
