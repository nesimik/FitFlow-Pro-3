package com.example.core

/**
 * Detaylı kas haritası.
 *
 * Uygulamanın ana kas grubu alanı (Göğüs / Sırt / Bacak / Omuz / Kol / Karın)
 * analiz için fazla kaba: "Kol" dediğinde biceps mi triceps mi çalıştı belli
 * olmuyor, "Sırt" dediğinde lat mı trapez mi bilinmiyor. Bu dosya her hareketi
 * 18 ayrı kas bölgesine birincil/ikincil olarak dağıtır.
 *
 * Hacim sayımı: birincil kas 1.0 set, ikincil kas 0.5 set alır. Ciddi hipertrofi
 * takip uygulamalarının kullandığı yaklaşım budur — bench press'i "sadece göğüs"
 * saymak triceps hacmini görünmez yapar.
 */
object MuscleMap {

    /* ------------------------------ Kas bölgeleri ------------------------------ */

    const val CHEST = "chest"
    const val FRONT_DELT = "front_delt"
    const val SIDE_DELT = "side_delt"
    const val REAR_DELT = "rear_delt"
    const val BICEPS = "biceps"
    const val TRICEPS = "triceps"
    const val FOREARM = "forearm"
    const val LATS = "lats"
    const val TRAPS = "traps"
    const val UPPER_BACK = "upper_back"
    const val LOWER_BACK = "lower_back"
    const val ABS = "abs"
    const val OBLIQUES = "obliques"
    const val GLUTES = "glutes"
    const val QUADS = "quads"
    const val HAMSTRINGS = "hamstrings"
    const val CALVES = "calves"
    const val ADDUCTORS = "adductors"

    /** Analizlerde ve haritada kullanılan tüm bölgeler, üstten aşağı mantıklı sırada. */
    val all = listOf(
        CHEST, FRONT_DELT, SIDE_DELT, REAR_DELT,
        LATS, UPPER_BACK, TRAPS, LOWER_BACK,
        BICEPS, TRICEPS, FOREARM,
        ABS, OBLIQUES,
        GLUTES, QUADS, HAMSTRINGS, ADDUCTORS, CALVES
    )

    /** Ön görünümde çizilen bölgeler. */
    val frontVisible = setOf(
        CHEST, FRONT_DELT, SIDE_DELT, BICEPS, FOREARM,
        ABS, OBLIQUES, QUADS, ADDUCTORS, CALVES, TRAPS
    )

    /** Arka görünümde çizilen bölgeler. */
    val backVisible = setOf(
        TRAPS, UPPER_BACK, LATS, LOWER_BACK, REAR_DELT, SIDE_DELT,
        TRICEPS, FOREARM, GLUTES, HAMSTRINGS, CALVES
    )

    fun label(key: String): String = when (key) {
        CHEST -> "Göğüs"
        FRONT_DELT -> "Ön omuz"
        SIDE_DELT -> "Yan omuz"
        REAR_DELT -> "Arka omuz"
        BICEPS -> "Biceps"
        TRICEPS -> "Triceps"
        FOREARM -> "Önkol"
        LATS -> "Kanat (lat)"
        TRAPS -> "Trapez"
        UPPER_BACK -> "Üst sırt"
        LOWER_BACK -> "Bel"
        ABS -> "Karın"
        OBLIQUES -> "Yan karın"
        GLUTES -> "Kalça"
        QUADS -> "Ön bacak"
        HAMSTRINGS -> "Arka bacak"
        CALVES -> "Baldır"
        ADDUCTORS -> "İç bacak"
        else -> key
    }

    /** Bölgenin ait olduğu ana kas grubu (mevcut renk paletiyle uyum için). */
    fun parentGroup(key: String): String = when (key) {
        CHEST -> Muscles.CHEST
        FRONT_DELT, SIDE_DELT, REAR_DELT -> Muscles.SHOULDERS
        BICEPS, TRICEPS, FOREARM -> Muscles.ARMS
        LATS, TRAPS, UPPER_BACK, LOWER_BACK -> Muscles.BACK
        ABS, OBLIQUES -> Muscles.CORE
        GLUTES, QUADS, HAMSTRINGS, CALVES, ADDUCTORS -> Muscles.LEGS
        else -> Muscles.CHEST
    }

    /**
     * Haftalık önerilen etkin set aralığı.
     * Hipertrofi literatüründe yaygın kabul gören aralıklar; kesin reçete değil,
     * "çok az / yeterli / çok fazla" ayrımı yapmak için referans.
     */
    fun weeklyTarget(key: String): IntRange = when (key) {
        CHEST -> 10..20
        LATS -> 10..20
        UPPER_BACK -> 8..16
        TRAPS -> 4..12
        LOWER_BACK -> 4..12
        FRONT_DELT -> 4..14
        SIDE_DELT -> 8..18
        REAR_DELT -> 6..14
        BICEPS -> 8..16
        TRICEPS -> 8..16
        FOREARM -> 2..10
        ABS -> 6..16
        OBLIQUES -> 4..12
        GLUTES -> 8..16
        QUADS -> 10..20
        HAMSTRINGS -> 8..16
        CALVES -> 6..16
        ADDUCTORS -> 2..10
        else -> 0..0
    }

    /** Kasın tipik toparlanma süresi (gün). Küçük kaslar daha çabuk toparlanır. */
    fun recoveryDays(key: String): Int = when (key) {
        QUADS, HAMSTRINGS, GLUTES, LOWER_BACK, LATS -> 3
        CHEST, UPPER_BACK, TRAPS -> 2
        FRONT_DELT, SIDE_DELT, REAR_DELT, BICEPS, TRICEPS -> 2
        ABS, OBLIQUES, CALVES, FOREARM, ADDUCTORS -> 1
        else -> 2
    }

    /* --------------------------- Hareket → kas eşlemesi -------------------------- */

    data class Activation(val primary: List<String>, val secondary: List<String> = emptyList()) {
        val isEmpty: Boolean get() = primary.isEmpty() && secondary.isEmpty()

        /** Set ağırlıkları: birincil 1.0, ikincil 0.5 */
        fun weights(): Map<String, Float> {
            val m = LinkedHashMap<String, Float>()
            primary.forEach { m[it] = 1f }
            secondary.forEach { if (!m.containsKey(it)) m[it] = 0.5f }
            return m
        }
    }

    private fun norm(s: String): String =
        s.lowercase(TR).replace("’", "'").replace(Regex("\\s+"), " ").trim()

    private val table: Map<String, Activation> by lazy {
        val m = LinkedHashMap<String, Activation>()
        fun e(name: String, primary: List<String>, secondary: List<String> = emptyList()) {
            m[norm(name)] = Activation(primary, secondary)
        }

        /* --------------------------------- Göğüs --------------------------------- */
        e("Bench Press", listOf(CHEST), listOf(FRONT_DELT, TRICEPS))
        e("Incline Bench Press", listOf(CHEST, FRONT_DELT), listOf(TRICEPS))
        e("Decline Bench Press", listOf(CHEST), listOf(TRICEPS, FRONT_DELT))
        e("Dumbbell Bench Press", listOf(CHEST), listOf(FRONT_DELT, TRICEPS))
        e("Incline Dumbbell Press", listOf(CHEST, FRONT_DELT), listOf(TRICEPS))
        e("Decline Dumbbell Press", listOf(CHEST), listOf(TRICEPS, FRONT_DELT))
        e("Floor Press", listOf(CHEST), listOf(TRICEPS, FRONT_DELT))
        e("Dumbbell Floor Press", listOf(CHEST), listOf(TRICEPS, FRONT_DELT))
        e("Dumbbell Fly", listOf(CHEST), listOf(FRONT_DELT))
        e("Incline Dumbbell Fly", listOf(CHEST), listOf(FRONT_DELT))
        e("Cable Crossover", listOf(CHEST), listOf(FRONT_DELT))
        e("Low to High Cable Fly", listOf(CHEST, FRONT_DELT), listOf())
        e("Pec Deck / Butterfly", listOf(CHEST), listOf(FRONT_DELT))
        e("Chest Press Makinesi", listOf(CHEST), listOf(TRICEPS, FRONT_DELT))
        e("Incline Chest Press Makinesi", listOf(CHEST, FRONT_DELT), listOf(TRICEPS))
        e("Dips (Göğüs)", listOf(CHEST, TRICEPS), listOf(FRONT_DELT))
        e("Ağırlıklı Dips", listOf(CHEST, TRICEPS), listOf(FRONT_DELT))
        e("Bench Dip", listOf(TRICEPS), listOf(CHEST, FRONT_DELT))
        e("Şınav (Push-Up)", listOf(CHEST), listOf(TRICEPS, FRONT_DELT, ABS))
        e("Diamond Push-Up", listOf(TRICEPS, CHEST), listOf(FRONT_DELT))
        e("Decline Push-Up", listOf(CHEST, FRONT_DELT), listOf(TRICEPS))
        e("Incline Push-Up", listOf(CHEST), listOf(TRICEPS, FRONT_DELT))
        e("Deficit Push-Up", listOf(CHEST), listOf(FRONT_DELT, TRICEPS))
        e("Pullover", listOf(LATS, CHEST), listOf(TRICEPS))
        e("Smith Machine Bench Press", listOf(CHEST), listOf(TRICEPS, FRONT_DELT))
        e("Smith Machine Incline Press", listOf(CHEST, FRONT_DELT), listOf(TRICEPS))
        e("Landmine Chest Press", listOf(CHEST, FRONT_DELT), listOf(TRICEPS))

        /* ---------------------------------- Sırt --------------------------------- */
        e("Deadlift", listOf(LOWER_BACK, GLUTES, HAMSTRINGS), listOf(TRAPS, QUADS, FOREARM, LATS))
        e("Romanian Deadlift", listOf(HAMSTRINGS, GLUTES), listOf(LOWER_BACK, FOREARM))
        e("Dumbbell Romanian Deadlift", listOf(HAMSTRINGS, GLUTES), listOf(LOWER_BACK, FOREARM))
        e("Sumo Deadlift", listOf(GLUTES, QUADS), listOf(LOWER_BACK, HAMSTRINGS, TRAPS, ADDUCTORS))
        e("Trap Bar Deadlift", listOf(QUADS, GLUTES, LOWER_BACK), listOf(TRAPS, FOREARM, LATS))
        e("Barbell Row", listOf(LATS, UPPER_BACK), listOf(BICEPS, REAR_DELT, LOWER_BACK))
        e("Pendlay Row", listOf(LATS, UPPER_BACK), listOf(BICEPS, TRAPS))
        e("T-Bar Row", listOf(LATS, UPPER_BACK), listOf(BICEPS, REAR_DELT))
        e("Chest Supported Row", listOf(UPPER_BACK, LATS), listOf(REAR_DELT, BICEPS))
        e("One Arm Dumbbell Row", listOf(LATS, UPPER_BACK), listOf(BICEPS, REAR_DELT))
        e("Bent Over Dumbbell Row", listOf(LATS, UPPER_BACK), listOf(BICEPS, REAR_DELT))
        e("Meadows Row", listOf(LATS, UPPER_BACK), listOf(REAR_DELT, BICEPS))
        e("Seal Row", listOf(UPPER_BACK, LATS), listOf(REAR_DELT, BICEPS))
        e("Kroc Row", listOf(LATS, UPPER_BACK, TRAPS), listOf(BICEPS, FOREARM))
        e("Barfiks (Pull-Up)", listOf(LATS), listOf(BICEPS, UPPER_BACK, FOREARM))
        e("Ağırlıklı Barfiks", listOf(LATS), listOf(BICEPS, UPPER_BACK, FOREARM))
        e("Chin-Up", listOf(LATS, BICEPS), listOf(UPPER_BACK, FOREARM))
        e("Neutral Grip Pull-Up", listOf(LATS, BICEPS), listOf(UPPER_BACK, FOREARM))
        e("Lat Pulldown", listOf(LATS), listOf(BICEPS, UPPER_BACK))
        e("Close Grip Pulldown", listOf(LATS), listOf(BICEPS))
        e("Reverse Grip Lat Pulldown", listOf(LATS, BICEPS), listOf(UPPER_BACK))
        e("Seated Cable Row", listOf(UPPER_BACK, LATS), listOf(BICEPS, REAR_DELT))
        e("Wide Grip Cable Row", listOf(UPPER_BACK, REAR_DELT), listOf(LATS, BICEPS))
        e("Single Arm Cable Row", listOf(LATS), listOf(BICEPS, UPPER_BACK))
        e("Straight Arm Pulldown", listOf(LATS), listOf(ABS))
        e("Face Pull", listOf(REAR_DELT, UPPER_BACK), listOf(TRAPS))
        e("Shrug (Barbell)", listOf(TRAPS), listOf(FOREARM))
        e("Shrug (Dumbbell)", listOf(TRAPS), listOf(FOREARM))
        e("Shrug", listOf(TRAPS), listOf(FOREARM))
        e("Hyperextension", listOf(LOWER_BACK, GLUTES), listOf(HAMSTRINGS))
        e("Good Morning", listOf(HAMSTRINGS, LOWER_BACK), listOf(GLUTES))
        e("Rack Pull", listOf(TRAPS, LOWER_BACK), listOf(GLUTES, FOREARM, LATS))
        e("Avustralya Barfiksi (Inverted Row)", listOf(UPPER_BACK, LATS), listOf(BICEPS))

        /* --------------------------------- Bacak --------------------------------- */
        e("Barbell Squat", listOf(QUADS, GLUTES), listOf(LOWER_BACK, HAMSTRINGS, ABS, ADDUCTORS))
        e("Front Squat", listOf(QUADS), listOf(GLUTES, ABS, UPPER_BACK))
        e("Goblet Squat", listOf(QUADS, GLUTES), listOf(ABS, ADDUCTORS))
        e("Hack Squat", listOf(QUADS), listOf(GLUTES))
        e("Leg Press", listOf(QUADS, GLUTES), listOf(HAMSTRINGS))
        e("Pendulum Squat", listOf(QUADS), listOf(GLUTES))
        e("Bulgarian Split Squat", listOf(QUADS, GLUTES), listOf(HAMSTRINGS, ADDUCTORS))
        e("Walking Lunge", listOf(QUADS, GLUTES), listOf(HAMSTRINGS, ADDUCTORS))
        e("Reverse Lunge", listOf(GLUTES, QUADS), listOf(HAMSTRINGS))
        e("Step Up", listOf(QUADS, GLUTES), listOf(HAMSTRINGS, CALVES))
        e("Leg Extension", listOf(QUADS))
        e("Leg Curl (Yatarak)", listOf(HAMSTRINGS), listOf(CALVES))
        e("Seated Leg Curl", listOf(HAMSTRINGS), listOf(CALVES))
        e("Standing Leg Curl", listOf(HAMSTRINGS), listOf(CALVES))
        e("Hip Thrust", listOf(GLUTES), listOf(HAMSTRINGS, ABS))
        e("Machine Hip Thrust", listOf(GLUTES), listOf(HAMSTRINGS))
        e("Glute Bridge", listOf(GLUTES), listOf(HAMSTRINGS))
        e("Cable Glute Kickback", listOf(GLUTES), listOf())
        e("Abduction (Kalça Açma)", listOf(GLUTES), listOf())
        e("Adduction (İç Bacak)", listOf(ADDUCTORS), listOf(QUADS))
        e("Calf Raise (Ayakta)", listOf(CALVES), listOf())
        e("Seated Calf Raise", listOf(CALVES), listOf())
        e("Donkey Calf Raise", listOf(CALVES), listOf())
        e("Tibialis Raise", listOf(CALVES), listOf())
        e("Sissy Squat", listOf(QUADS), listOf())
        e("Bodyweight Squat", listOf(QUADS, GLUTES))
        e("Wall Sit", listOf(QUADS), listOf(GLUTES))
        e("Nordic Curl", listOf(HAMSTRINGS), listOf(GLUTES, CALVES))
        e("Pistol Squat", listOf(QUADS, GLUTES), listOf(HAMSTRINGS))

        /* ---------------------------------- Omuz --------------------------------- */
        e("Overhead Press", listOf(FRONT_DELT, SIDE_DELT), listOf(TRICEPS, TRAPS, ABS))
        e("Push Press", listOf(FRONT_DELT), listOf(TRICEPS, QUADS, TRAPS))
        e("Seated Barbell OHP", listOf(FRONT_DELT, SIDE_DELT), listOf(TRICEPS))
        e("Dumbbell Shoulder Press", listOf(FRONT_DELT, SIDE_DELT), listOf(TRICEPS))
        e("Arnold Press", listOf(FRONT_DELT, SIDE_DELT), listOf(TRICEPS))
        e("Machine Shoulder Press", listOf(FRONT_DELT, SIDE_DELT), listOf(TRICEPS))
        e("Smith Machine Shoulder Press", listOf(FRONT_DELT, SIDE_DELT), listOf(TRICEPS))
        e("Lateral Raise", listOf(SIDE_DELT), listOf(TRAPS))
        e("Cable Lateral Raise", listOf(SIDE_DELT), listOf(TRAPS))
        e("Egyptian Cable Lateral Raise", listOf(SIDE_DELT), listOf(TRAPS))
        e("Machine Lateral Raise", listOf(SIDE_DELT), listOf())
        e("Front Raise (Dumbbell)", listOf(FRONT_DELT), listOf(CHEST))
        e("Front Raise (Plate/Plaka)", listOf(FRONT_DELT), listOf(CHEST))
        e("Cable Front Raise", listOf(FRONT_DELT), listOf(CHEST))
        e("Front Raise", listOf(FRONT_DELT), listOf(CHEST))
        e("Rear Delt Fly (Dumbbell)", listOf(REAR_DELT), listOf(UPPER_BACK))
        e("Rear Delt Fly", listOf(REAR_DELT), listOf(UPPER_BACK))
        e("Reverse Pec Deck", listOf(REAR_DELT), listOf(UPPER_BACK))
        e("Cable Rear Delt Cross", listOf(REAR_DELT), listOf(UPPER_BACK))
        e("Upright Row (Barbell)", listOf(SIDE_DELT, TRAPS), listOf(BICEPS))
        e("Upright Row (Dumbbell)", listOf(SIDE_DELT, TRAPS), listOf(BICEPS))
        e("Cable Upright Row", listOf(SIDE_DELT, TRAPS), listOf(BICEPS))
        e("Upright Row", listOf(SIDE_DELT, TRAPS), listOf(BICEPS))
        e("Pike Push-Up", listOf(FRONT_DELT), listOf(TRICEPS, SIDE_DELT))
        e("Handstand Push-Up (Amut Şınav)", listOf(FRONT_DELT, SIDE_DELT), listOf(TRICEPS, TRAPS))

        /* ----------------------------------- Kol --------------------------------- */
        e("Barbell Curl", listOf(BICEPS), listOf(FOREARM))
        e("EZ-Bar Curl", listOf(BICEPS), listOf(FOREARM))
        e("Dumbbell Curl", listOf(BICEPS), listOf(FOREARM))
        e("Incline Dumbbell Curl", listOf(BICEPS), listOf())
        e("Spider Curl", listOf(BICEPS), listOf())
        e("Preacher Curl (Scott Curl)", listOf(BICEPS), listOf())
        e("Preacher Curl", listOf(BICEPS), listOf())
        e("Dumbbell Preacher Curl", listOf(BICEPS), listOf())
        e("Concentration Curl", listOf(BICEPS), listOf())
        e("Hammer Curl", listOf(BICEPS, FOREARM), listOf())
        e("Cable Hammer Curl", listOf(BICEPS, FOREARM), listOf())
        e("Cross Body Hammer Curl", listOf(BICEPS, FOREARM), listOf())
        e("Cable Curl", listOf(BICEPS), listOf(FOREARM))
        e("High Cable Curl (Herakles)", listOf(BICEPS), listOf())
        e("Reverse Curl", listOf(FOREARM, BICEPS), listOf())
        e("21s Biceps Curl", listOf(BICEPS), listOf(FOREARM))

        e("Skull Crusher (Alna Triceps)", listOf(TRICEPS), listOf())
        e("Skull Crusher", listOf(TRICEPS), listOf())
        e("Dumbbell Skull Crusher", listOf(TRICEPS), listOf())
        e("Close Grip Bench Press", listOf(TRICEPS), listOf(CHEST, FRONT_DELT))
        e("Triceps Pushdown (Düz Bar)", listOf(TRICEPS), listOf())
        e("Triceps Pushdown", listOf(TRICEPS), listOf())
        e("Rope Pushdown (Halat)", listOf(TRICEPS), listOf())
        e("Rope Pushdown", listOf(TRICEPS), listOf())
        e("V-Bar Pushdown", listOf(TRICEPS), listOf())
        e("Overhead Cable Triceps Extension", listOf(TRICEPS), listOf())
        e("Overhead Triceps Extension", listOf(TRICEPS), listOf())
        e("Overhead Dumbbell Extension", listOf(TRICEPS), listOf())
        e("Single Arm Overhead DB Extension", listOf(TRICEPS), listOf())
        e("Triceps Kickback (Dumbbell)", listOf(TRICEPS), listOf())
        e("Cable Triceps Kickback", listOf(TRICEPS), listOf())
        e("Triceps Kickback", listOf(TRICEPS), listOf())
        e("Dips (Triceps Odaklı)", listOf(TRICEPS), listOf(CHEST, FRONT_DELT))
        e("JM Press", listOf(TRICEPS), listOf(CHEST))
        e("Tate Press", listOf(TRICEPS), listOf())

        e("Wrist Curl (Bilek Bükme)", listOf(FOREARM), listOf())
        e("Wrist Curl", listOf(FOREARM), listOf())
        e("Reverse Wrist Curl", listOf(FOREARM), listOf())
        e("Farmer's Walk", listOf(FOREARM, TRAPS), listOf(ABS, QUADS))
        e("Plate Pinch Hold", listOf(FOREARM), listOf())

        /* ---------------------------------- Karın -------------------------------- */
        e("Plank", listOf(ABS), listOf(OBLIQUES, LOWER_BACK))
        e("Side Plank", listOf(OBLIQUES), listOf(ABS))
        e("RKC Plank", listOf(ABS, OBLIQUES), listOf(LOWER_BACK))
        e("Crunch (Mekik)", listOf(ABS), listOf())
        e("Crunch", listOf(ABS), listOf())
        e("Decline Bench Crunch", listOf(ABS), listOf())
        e("Weighted Crunch", listOf(ABS), listOf())
        e("Cable Crunch", listOf(ABS), listOf(OBLIQUES))
        e("Bicycle Crunch", listOf(ABS, OBLIQUES), listOf())
        e("Russian Twist", listOf(OBLIQUES), listOf(ABS))
        e("Leg Raise (Yerde)", listOf(ABS), listOf(OBLIQUES))
        e("Leg Raise", listOf(ABS), listOf(OBLIQUES))
        e("Hanging Leg Raise", listOf(ABS), listOf(OBLIQUES, FOREARM))
        e("Hanging Knee Raise", listOf(ABS), listOf(OBLIQUES))
        e("Ab Wheel Rollout", listOf(ABS), listOf(LATS, LOWER_BACK))
        e("Dead Bug", listOf(ABS), listOf(LOWER_BACK))
        e("Bird Dog", listOf(LOWER_BACK), listOf(ABS, GLUTES))
        e("Hollow Body Hold", listOf(ABS), listOf())
        e("Pallof Press", listOf(OBLIQUES), listOf(ABS))
        e("Cable Woodchopper", listOf(OBLIQUES), listOf(ABS))
        e("Mountain Climber", listOf(ABS), listOf(QUADS))
        e("V-Up", listOf(ABS), listOf())
        e("Toe to Bar", listOf(ABS), listOf(FOREARM))
        e("Mide Vakumu (Stomach Vacuum)", listOf(ABS), listOf())

        /* --------------------------- Kardiyo & mobilite -------------------------- */
        e("Eğimli Yürüyüş", emptyList(), listOf(CALVES, GLUTES))
        e("Koşu", emptyList(), listOf(QUADS, CALVES, HAMSTRINGS))
        e("Sprint / HIIT", emptyList(), listOf(QUADS, CALVES, HAMSTRINGS, GLUTES))
        e("Bisiklet", emptyList(), listOf(QUADS, CALVES))
        e("Spin Bike", emptyList(), listOf(QUADS, CALVES, GLUTES))
        e("AirBike / Assault Bike", emptyList(), listOf(QUADS, CALVES, CHEST, LATS))
        e("Kürek Makinesi", emptyList(), listOf(LATS, UPPER_BACK, QUADS))
        e("İp Atlama", emptyList(), listOf(CALVES))
        e("Burpee", emptyList(), listOf(QUADS, CHEST, ABS))
        e("Kettlebell Swing", listOf(GLUTES, HAMSTRINGS), listOf(LOWER_BACK, FRONT_DELT))
        e("Box Jump", listOf(QUADS, GLUTES), listOf(CALVES))
        e("Battle Ropes (Halat)", emptyList(), listOf(SIDE_DELT, FRONT_DELT, ABS))
        e("Sled Push (Kızak İtme)", listOf(QUADS, GLUTES), listOf(CALVES))
        e("Slam Ball", emptyList(), listOf(ABS, FRONT_DELT, LATS))
        e("Jumping Jack", emptyList(), listOf(CALVES, SIDE_DELT))
        e("Merdiven / StairMaster", emptyList(), listOf(GLUTES, QUADS, CALVES))

        e("Arm Circles", emptyList(), listOf(SIDE_DELT))
        e("Shoulder Dislocates", emptyList(), listOf(FRONT_DELT, CHEST))
        e("Band Pull Apart", emptyList(), listOf(REAR_DELT, UPPER_BACK))
        e("Scapula Push-Up", emptyList(), listOf(UPPER_BACK))
        e("Cat-Cow", emptyList(), listOf(LOWER_BACK))
        e("World's Greatest Stretch", emptyList(), listOf(GLUTES, HAMSTRINGS, UPPER_BACK))
        e("90/90 Kalça Açma", emptyList(), listOf(GLUTES))
        e("Hip Hinge Drill", emptyList(), listOf(HAMSTRINGS))
        e("Cossack Squat", emptyList(), listOf(ADDUCTORS, QUADS, GLUTES))
        e("Couch Stretch", emptyList(), listOf(QUADS))
        e("Thoracic Rotation", emptyList(), listOf(UPPER_BACK))
        e("Pigeon Pose (Güvercin Duruşu)", emptyList(), listOf(GLUTES))
        e("Ölü Asılma (Dead Hang)", emptyList(), listOf(LATS, FOREARM))
        e("Boş Bar Squat", emptyList(), listOf(QUADS))
        e("Boş Bar Bench", emptyList(), listOf(CHEST))
        e("Boş Bar OHP", emptyList(), listOf(FRONT_DELT))

        m
    }

    /**
     * Hareket adından (ve gerekirse ana kas grubundan) kas aktivasyonunu bulur.
     * Tabloda yoksa ada göre anahtar kelime tahmini, o da tutmazsa ana kas
     * grubunun varsayılan dağılımı kullanılır.
     */
    fun resolve(name: String, muscleGroup: String = "", secondaryMuscles: String = ""): Activation {
        table[norm(name)]?.let { return it }

        val n = norm(name)
        fun has(vararg keys: String) = keys.any { n.contains(it) }

        // İsimden tahmin
        val guessed: Activation? = when {
            has("bench", "göğüs", "chest", "fly", "flye", "pec", "şınav", "push-up", "push up") ->
                Activation(listOf(CHEST), listOf(TRICEPS, FRONT_DELT))
            has("dip") -> Activation(listOf(CHEST, TRICEPS), listOf(FRONT_DELT))
            has("deadlift", "hinge") -> Activation(listOf(HAMSTRINGS, GLUTES, LOWER_BACK), listOf(TRAPS, FOREARM))
            has("pulldown", "pull-up", "pull up", "barfiks", "chin-up", "chin up", "chinup", "lat ") || n.split(" ").any { it == "chin" || it == "chins" } ->
                Activation(listOf(LATS), listOf(BICEPS, UPPER_BACK))
            has("row", "kürek") -> Activation(listOf(LATS, UPPER_BACK), listOf(BICEPS, REAR_DELT))
            has("shrug", "trapez") -> Activation(listOf(TRAPS))
            has("hyperextension", "bel", "back extension") -> Activation(listOf(LOWER_BACK), listOf(GLUTES))
            has("squat") -> Activation(listOf(QUADS, GLUTES), listOf(HAMSTRINGS, LOWER_BACK))
            has("lunge", "split squat", "step up") -> Activation(listOf(QUADS, GLUTES), listOf(HAMSTRINGS))
            has("leg press") -> Activation(listOf(QUADS, GLUTES))
            has("leg extension") -> Activation(listOf(QUADS))
            has("leg curl", "hamstring", "nordic") -> Activation(listOf(HAMSTRINGS))
            has("hip thrust", "glute", "kalça", "abduction") -> Activation(listOf(GLUTES), listOf(HAMSTRINGS))
            has("calf", "baldır") -> Activation(listOf(CALVES))
            has("lateral raise", "yan omuz", "side delt") -> Activation(listOf(SIDE_DELT), listOf(TRAPS))
            has("rear delt", "reverse pec", "face pull", "arka omuz") -> Activation(listOf(REAR_DELT), listOf(UPPER_BACK))
            has("front raise", "ön omuz") -> Activation(listOf(FRONT_DELT))
            has("overhead press", "ohp", "shoulder press", "arnold", "push press", "omuz") ->
                Activation(listOf(FRONT_DELT, SIDE_DELT), listOf(TRICEPS))
            has("upright row") -> Activation(listOf(SIDE_DELT, TRAPS))
            has("curl") && !has("leg") -> Activation(listOf(BICEPS), listOf(FOREARM))
            has("triceps", "pushdown", "skull", "kickback", "extension") -> Activation(listOf(TRICEPS))
            has("wrist", "önkol", "grip", "farmer") -> Activation(listOf(FOREARM))
            has("plank", "hollow", "dead bug", "crunch", "mekik", "leg raise", "ab wheel", "karın") ->
                Activation(listOf(ABS), listOf(OBLIQUES))
            has("oblique", "russian twist", "side plank", "pallof", "yan karın") -> Activation(listOf(OBLIQUES), listOf(ABS))
            has("koş", "yürüy", "bisiklet", "run", "walk", "bike", "cardio", "kardiyo", "jump", "ip atlama") ->
                Activation(emptyList(), listOf(QUADS, CALVES))
            else -> null
        }
        if (guessed != null) return guessed

        // Ana kas grubuna göre varsayılan
        return when (muscleGroup) {
            Muscles.CHEST -> Activation(listOf(CHEST), listOf(TRICEPS, FRONT_DELT))
            Muscles.BACK -> Activation(listOf(LATS, UPPER_BACK), listOf(BICEPS))
            Muscles.LEGS -> Activation(listOf(QUADS, GLUTES), listOf(HAMSTRINGS))
            Muscles.SHOULDERS -> Activation(listOf(FRONT_DELT, SIDE_DELT), listOf(TRICEPS))
            Muscles.ARMS -> Activation(listOf(BICEPS, TRICEPS))
            Muscles.CORE -> Activation(listOf(ABS), listOf(OBLIQUES))
            else -> Activation(emptyList(), emptyList())
        }
    }

    /** Hareket için kısa okunabilir özet: "Göğüs, Ön omuz · Triceps" */
    fun summary(name: String, muscleGroup: String = ""): String {
        val a = resolve(name, muscleGroup)
        val p = a.primary.joinToString(", ") { label(it) }
        val s = a.secondary.joinToString(", ") { label(it) }
        return when {
            p.isBlank() && s.isBlank() -> "—"
            s.isBlank() -> p
            p.isBlank() -> "Destek: $s"
            else -> "$p  ·  $s"
        }
    }
}
