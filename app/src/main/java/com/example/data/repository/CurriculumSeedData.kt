package com.example.data.repository

import com.example.core.database.*
import com.example.core.model.QuestionType

object CurriculumSeedData {

    fun getInitialClasses(): List<ClassEntity> {
        val bengaliClassNames = listOf(
            "প্রথম শ্রেণি", "দ্বিতীয় শ্রেণি", "তৃতীয় শ্রেণি", "চতুর্থ শ্রেণি", "পঞ্চম শ্রেণি",
            "ষষ্ঠ শ্রেণি", "সপ্তম শ্রেণি", "অষ্টম শ্রেণি", "নবম শ্রেণি", "দশম শ্রেণি"
        )
        return (1..10).map { i ->
            ClassEntity(
                id = "class_$i",
                name = "Class $i",
                bengaliName = bengaliClassNames[i - 1],
                order = i,
                iconName = "school",
                isActive = true
            )
        }
    }

    fun getInitialSubjects(): List<SubjectEntity> {
        val list = mutableListOf<SubjectEntity>()
        for (i in 1..10) {
            val classId = "class_$i"
            list.add(SubjectEntity("sub_${classId}_bangla", classId, "Bangla", "বাংলা", "book", "#2563EB", 1, true))
            list.add(SubjectEntity("sub_${classId}_english", classId, "English", "ইংরেজি", "translate", "#7C3AED", 2, true))
            list.add(SubjectEntity("sub_${classId}_math", classId, "Mathematics", "গণিত", "calculate", "#059669", 3, true))
            list.add(SubjectEntity("sub_${classId}_science", classId, "General Science", "সাধারণ বিজ্ঞান", "science", "#D97706", 4, true))
            if (i >= 6) {
                list.add(SubjectEntity("sub_${classId}_ict", classId, "ICT", "তথ্য ও যোগাযোগ প্রযুক্তি", "computer", "#DC2626", 5, true))
                list.add(SubjectEntity("sub_${classId}_bgs", classId, "Bangladesh & Global Studies", "বাংলাদেশ ও বিশ্বপরিচয়", "public", "#0891B2", 6, true))
            }
        }
        return list
    }

    fun getInitialChapters(): List<ChapterEntity> {
        val list = mutableListOf<ChapterEntity>()
        // Math Class 9
        list.add(ChapterEntity("chap_c9_m1", "sub_class_9_math", "class_9", "Real Numbers", "বাস্তব সংখ্যা", 1, "মূলদ ও অমূলদ সংখ্যার ধারণা", 1, true))
        list.add(ChapterEntity("chap_c9_m2", "sub_class_9_math", "class_9", "Sets and Functions", "সেট ও ফাংশন", 2, "সেটের ধর্ম ও ভেনচিত্র", 2, true))
        list.add(ChapterEntity("chap_c9_m3", "sub_class_9_math", "class_9", "Algebraic Expressions", "বীজগণিতীয় রাশি", 3, "বর্গ ও ঘন সংবলিত সূত্রাবলী", 3, true))

        // Bangla Class 9
        list.add(ChapterEntity("chap_c9_b1", "sub_class_9_bangla", "class_9", "Shuva", "শুভা - রবীন্দ্রনাথ ঠাকুর", 1, "গল্প ও চরিত্র বিশ্লেষণ", 1, true))
        list.add(ChapterEntity("chap_c9_b2", "sub_class_9_bangla", "class_9", "Boi Pora", "বই পড়া - প্রমথ চৌধুরী", 2, "শিক্ষা ও সাহিত্যের গুরুত্ব", 2, true))

        // Science Class 9
        list.add(ChapterEntity("chap_c9_s1", "sub_class_9_science", "class_9", "Physical World and Measurement", "ভৌত রাশি ও পরিমাপ", 1, "মৌলিক ও লব্ধ রাশির একক", 1, true))
        list.add(ChapterEntity("chap_c9_s2", "sub_class_9_science", "class_9", "Motion", "গতি", 2, "দূরত্ব, সরণ, বেগ ও ত্বরণ", 2, true))

        // English Class 9
        list.add(ChapterEntity("chap_c9_e1", "sub_class_9_english", "class_9", "Good Citizens", "Unit 1: Good Citizens", 1, "Values and responsibilities", 1, true))
        list.add(ChapterEntity("chap_c9_e2", "sub_class_9_english", "class_9", "Pastimes", "Unit 2: Pastimes", 2, "Hobbies and recreation", 2, true))

        // Math Class 10
        list.add(ChapterEntity("chap_c10_m1", "sub_class_10_math", "class_10", "Trigonometric Ratio", "ত্রিকোণমিতিক অনুপাত", 10, "সূক্ষ্মকোণের ত্রিকোণমিতিক অনুপাত", 1, true))

        // Class 8 Math
        list.add(ChapterEntity("chap_c8_m1", "sub_class_8_math", "class_8", "Patterns", "প্যাটার্ন", 1, "মৌলিক প্যাটার্ন ও বীজগণিতীয় রূপ", 1, true))

        return list
    }

    fun getInitialQuizzes(): List<QuizEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            QuizEntity(
                id = "quiz_c9_m1_q1",
                chapterId = "chap_c9_m1",
                subjectId = "sub_class_9_math",
                classId = "class_9",
                title = "বাস্তব সংখ্যা বেসিক যাচাই",
                description = "মূলদ, অমূলদ ও আবৃত্ত দশমিক ভগ্নাংশ সম্পর্কিত গুরুত্বপূর্ণ কুইজ",
                difficulty = "easy",
                timeLimitSeconds = 300,
                shuffleQuestions = true,
                shuffleOptions = true,
                isPublished = true,
                questionsCount = 5,
                totalPoints = 5,
                createdAt = now,
                updatedAt = now
            ),
            QuizEntity(
                id = "quiz_c9_m3_q1",
                chapterId = "chap_c9_m3",
                subjectId = "sub_class_9_math",
                classId = "class_9",
                title = "বীজগণিতীয় সূত্রাবলী ও প্রয়োগ",
                description = "বর্গ ও ঘনের সূত্রাবলী নিয়ে অনুশীলন ও মডেল টেস্ট",
                difficulty = "medium",
                timeLimitSeconds = 420,
                shuffleQuestions = true,
                shuffleOptions = true,
                isPublished = true,
                questionsCount = 5,
                totalPoints = 5,
                createdAt = now,
                updatedAt = now
            ),
            QuizEntity(
                id = "quiz_c9_b1_q1",
                chapterId = "chap_c9_b1",
                subjectId = "sub_class_9_bangla",
                classId = "class_9",
                title = "শুভা গল্পের পাঠ যাচাই",
                description = "শুভা গল্পের মূলভাব ও চরিত্রভিত্তিক সংক্ষিপ্ত কুইজ",
                difficulty = "easy",
                timeLimitSeconds = 240,
                shuffleQuestions = true,
                shuffleOptions = true,
                isPublished = true,
                questionsCount = 4,
                totalPoints = 4,
                createdAt = now,
                updatedAt = now
            ),
            QuizEntity(
                id = "quiz_c9_s2_q1",
                chapterId = "chap_c9_s2",
                subjectId = "sub_class_9_science",
                classId = "class_9",
                title = "গতির সূত্রাবলী ও মাত্রা",
                description = "বেগ, ত্বরণ ও গতির সমীকরণ সম্পর্কিত এমসিকিউ টেস্ট",
                difficulty = "hard",
                timeLimitSeconds = 360,
                shuffleQuestions = true,
                shuffleOptions = true,
                isPublished = true,
                questionsCount = 4,
                totalPoints = 4,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    fun getInitialQuestions(): List<QuestionEntity> {
        return listOf(
            // Quiz 1: বাস্তব সংখ্যা
            QuestionEntity(
                id = "q_m1_1",
                quizId = "quiz_c9_m1_q1",
                type = QuestionType.MCQ.name.lowercase(),
                questionText = "নিচের কোনটি অমূলদ সংখ্যা?",
                options = listOf("√4", "√9", "√2", "4.5"),
                answer = "√2",
                acceptedAnswers = listOf("√2", "root 2", "sq root 2"),
                explanation = "√2 কে দুটি পূর্ণসংখ্যার ভগ্নাংশ আকারে প্রকাশ করা যায় না, তাই এটি অমূলদ সংখ্যা। অপরপক্ষে √4=2 এবং √9=3 মূলদ সংখ্যা।",
                points = 1,
                imageUrl = null,
                order = 1
            ),
            QuestionEntity(
                id = "q_m1_2",
                quizId = "quiz_c9_m1_q1",
                type = QuestionType.FILL_BLANK.name.lowercase(),
                questionText = "যে সকল সংখ্যার কেবল ১ এবং ঐ সংখ্যা নিজেই গুণনীয়ক, সেগুলোকে কী সংখ্যা বলা হয়?",
                options = emptyList(),
                answer = "মৌলিক",
                acceptedAnswers = listOf("মৌলিক সংখ্যা", "মৌলিক", "prime", "prime number"),
                explanation = "মৌলিক সংখ্যার মাত্র দুটি পৃথক গুণনীয়ক থাকে: ১ এবং সেই সংখ্যা নিজে (যেমন ২, ৩, ৫, ৭)।",
                points = 1,
                imageUrl = null,
                order = 2
            ),
            QuestionEntity(
                id = "q_m1_3",
                quizId = "quiz_c9_m1_q1",
                type = QuestionType.MCQ.name.lowercase(),
                questionText = "সবচেয়ে ছোট মৌলিক সংখ্যা কোনটি?",
                options = listOf("০", "১", "২", "৩"),
                answer = "২",
                acceptedAnswers = listOf("২", "2", "two"),
                explanation = "২ একমাত্র জোড় মৌলিক সংখ্যা এবং এটিই সবথেকে ক্ষুদ্রতম মৌলিক সংখ্যা। ১ মৌলিক বা যৌগিক কোনোটিই নয়।",
                points = 1,
                imageUrl = null,
                order = 3
            ),
            QuestionEntity(
                id = "q_m1_4",
                quizId = "quiz_c9_m1_q1",
                type = QuestionType.MCQ.name.lowercase(),
                questionText = "০.৩̇ (৩ আবৃত্ত দশমিক) এর সাধারণ ভগ্নাংশ রূপ কোনটি?",
                options = listOf("১/৩", "৩/১০", "১/৯", "৩/৭"),
                answer = "১/৩",
                acceptedAnswers = listOf("১/৩", "1/3"),
                explanation = "০.৩̇ = ৩/৯ = ১/৩।",
                points = 1,
                imageUrl = null,
                order = 4
            ),
            QuestionEntity(
                id = "q_m1_5",
                quizId = "quiz_c9_m1_q1",
                type = QuestionType.FILL_BLANK.name.lowercase(),
                questionText = "ধনাত্মক ও ঋণাত্মক পূর্ণসংখ্যা এবং শূন্যকে একত্রে কী সংখ্যা বলে?",
                options = emptyList(),
                answer = "পূর্ণসংখ্যা",
                acceptedAnswers = listOf("পূর্ণসংখ্যা", "পূর্ণ সংখ্যা", "integer", "integers"),
                explanation = "শূন্যসহ সকল ধনাত্মক ও ঋণাত্মক অখণ্ড সংখ্যাকে পূর্ণসংখ্যা (Integer) বলা হয়।",
                points = 1,
                imageUrl = null,
                order = 5
            ),

            // Quiz 2: বীজগণিতীয় রাশি
            QuestionEntity(
                id = "q_m3_1",
                quizId = "quiz_c9_m3_q1",
                type = QuestionType.MCQ.name.lowercase(),
                questionText = "a + b = 5 এবং a - b = 3 হলে, 4ab এর মান কত?",
                options = listOf("16", "20", "24", "15"),
                answer = "16",
                acceptedAnswers = listOf("16", "১৬", "sixteen"),
                explanation = "আমরা জানি, 4ab = (a + b)² - (a - b)² = 5² - 3² = 25 - 9 = 16।",
                points = 1,
                imageUrl = null,
                order = 1
            ),
            QuestionEntity(
                id = "q_m3_2",
                quizId = "quiz_c9_m3_q1",
                type = QuestionType.FILL_BLANK.name.lowercase(),
                questionText = "a² - b² এর উৎপাদক বিশ্লেষণ রূপ কী? (e.g. (a+b)(a-b))",
                options = emptyList(),
                answer = "(a+b)(a-b)",
                acceptedAnswers = listOf("(a+b)(a-b)", "(a-b)(a+b)", "(a + b)(a - b)"),
                explanation = "সূত্র অনুযায়ী: a² - b² = (a + b)(a - b)।",
                points = 1,
                imageUrl = null,
                order = 2
            ),
            QuestionEntity(
                id = "q_m3_3",
                quizId = "quiz_c9_m3_q1",
                type = QuestionType.MCQ.name.lowercase(),
                questionText = "যদি x + 1/x = 2 হয়, তবে x² + 1/x² এর মান কত?",
                options = listOf("2", "4", "0", "1"),
                answer = "2",
                acceptedAnswers = listOf("2", "২", "two"),
                explanation = "x² + 1/x² = (x + 1/x)² - 2 = 2² - 2 = 4 - 2 = 2।",
                points = 1,
                imageUrl = null,
                order = 3
            ),
            QuestionEntity(
                id = "q_m3_4",
                quizId = "quiz_c9_m3_q1",
                type = QuestionType.MCQ.name.lowercase(),
                questionText = "(a + b)³ এর সূত্র কোনটি?",
                options = listOf(
                    "a³ + 3a²b + 3ab² + b³",
                    "a³ - 3a²b + 3ab² - b³",
                    "a³ + b³ + 3ab",
                    "a³ + b³"
                ),
                answer = "a³ + 3a²b + 3ab² + b³",
                acceptedAnswers = listOf("a³ + 3a²b + 3ab² + b³"),
                explanation = "(a + b)³ = a³ + 3a²b + 3ab² + b³ = a³ + b³ + 3ab(a + b)।",
                points = 1,
                imageUrl = null,
                order = 4
            ),
            QuestionEntity(
                id = "q_m3_5",
                quizId = "quiz_c9_m3_q1",
                type = QuestionType.FILL_BLANK.name.lowercase(),
                questionText = "২ + ২ কত হয়?",
                options = emptyList(),
                answer = "৪",
                acceptedAnswers = listOf("4", "৪", "চার", "four"),
                explanation = "২ + ২ = ৪।",
                points = 1,
                imageUrl = null,
                order = 5
            ),

            // Quiz 3: শুভা
            QuestionEntity(
                id = "q_b1_1",
                quizId = "quiz_c9_b1_q1",
                type = QuestionType.MCQ.name.lowercase(),
                questionText = "‘শুভা’ গল্পের রচয়িতা কে?",
                options = listOf("কাজী নজরুল ইসলাম", "রবীন্দ্রনাথ ঠাকুর", "বঙ্কিমচন্দ্র চট্টোপাধ্যায়", "শরৎচন্দ্র চট্টোপাধ্যায়"),
                answer = "রবীন্দ্রনাথ ঠাকুর",
                acceptedAnswers = listOf("রবীন্দ্রনাথ ঠাকুর", "Rabindranath Tagore", "রবীন্দ্রনাথ"),
                explanation = "‘শুভা’ রবীন্দ্রনাথ ঠাকুরের বিখ্যাত একটি ছোটগল্প, যেখানে বাকপ্রতিবন্ধী এক কিশোরীর মর্মস্পর্শী অনুভূতি চিত্রিত হয়েছে।",
                points = 1,
                imageUrl = null,
                order = 1
            ),
            QuestionEntity(
                id = "q_b1_2",
                quizId = "quiz_c9_b1_q1",
                type = QuestionType.FILL_BLANK.name.lowercase(),
                questionText = "শুভার আসল নাম কী ছিল?",
                options = emptyList(),
                answer = "সুভাষিণী",
                acceptedAnswers = listOf("সুভাষিণী", "শুভাষিনী", "Subhashini"),
                explanation = "শুভার পুরো নাম সুভাষিণী। বড় দুই বোনের নাম সুকেশিনী ও সুহাসিনী রাখার কারণে মিলিয়ে তার এই নাম রাখা হয়েছিল।",
                points = 1,
                imageUrl = null,
                order = 2
            ),
            QuestionEntity(
                id = "q_b1_3",
                quizId = "quiz_c9_b1_q1",
                type = QuestionType.MCQ.name.lowercase(),
                questionText = "শুভার দুটি পোষা গাভীর নাম কী ছিল?",
                options = listOf("সর্বশী ও পাঙ্গুলি", "রাঙি ও ধবলি", "সুরভী ও বিজলী", "কাউরী ও শ্যামলী"),
                answer = "সর্বশী ও পাঙ্গুলি",
                acceptedAnswers = listOf("সর্বশী ও পাঙ্গুলি"),
                explanation = "শুভার গোয়ালে দুটি গাভী ছিল যাদের নাম সর্বশী ও পাঙ্গুলি। তারা শুভার মুখের ভাব খুব ভালো বুঝত।",
                points = 1,
                imageUrl = null,
                order = 3
            ),
            QuestionEntity(
                id = "q_b1_4",
                quizId = "quiz_c9_b1_q1",
                type = QuestionType.MCQ.name.lowercase(),
                questionText = "শুভার গ্রামের নাম কী ছিল?",
                options = listOf("চণ্ডীপুর", "কামারপাড়া", "শিবপুর", "মোহনপুর"),
                answer = "চণ্ডীপুর",
                acceptedAnswers = listOf("চণ্ডীপুর"),
                explanation = "শুবাদের বাড়ি ছিল নদী তীরবর্তী চণ্ডীপুর গ্রামে।",
                points = 1,
                imageUrl = null,
                order = 4
            ),

            // Quiz 4: গতির সূত্রাবলী
            QuestionEntity(
                id = "q_s2_1",
                quizId = "quiz_c9_s2_q1",
                type = QuestionType.MCQ.name.lowercase(),
                questionText = "ত্বরণের এসআই (SI) একক কোনটি?",
                options = listOf("m/s", "m/s²", "N/kg", "m²"),
                answer = "m/s²",
                acceptedAnswers = listOf("m/s²", "ms^-2", "মিটার/সেকেন্ড²"),
                explanation = "বেগ পরিবর্তনের হারকে ত্বরণ বলে। এর একক হলো মিটার প্রতি সেকেন্ড স্কয়ার (m/s²)।",
                points = 1,
                imageUrl = null,
                order = 1
            ),
            QuestionEntity(
                id = "q_s2_2",
                quizId = "quiz_c9_s2_q1",
                type = QuestionType.FILL_BLANK.name.lowercase(),
                questionText = "নির্দিষ্ট দিকে কোনো বস্তুর অবস্থানের পরিবর্তনকে কী বলে?",
                options = emptyList(),
                answer = "সরণ",
                acceptedAnswers = listOf("সরণ", "displacement"),
                explanation = "নির্দিষ্ট দিকে পারিপার্শ্বিকের সাপেক্ষে বস্তুর অবস্থান পরিবর্তনের সরলরৈখিক দূরত্বকে সরণ (Displacement) বলে।",
                points = 1,
                imageUrl = null,
                order = 2
            ),
            QuestionEntity(
                id = "q_s2_3",
                quizId = "quiz_c9_s2_q1",
                type = QuestionType.MCQ.name.lowercase(),
                questionText = "স্থির অবস্থান থেকে সুষম ত্বরণে চলমান বস্তুর গতির সমীকরণ কোনটি?",
                options = listOf("v = u + at", "s = ut + ½at²", "v² = u² + 2as", "উপরের সবকটি"),
                answer = "উপরের সবকটি",
                acceptedAnswers = listOf("উপরের সবকটি", "all", "all of the above"),
                explanation = "v = u + at, s = ut + ½at² এবং v² = u² + 2as প্রতিটি সমীকরণই সুষম ত্বরণে চলমান বস্তুর ক্ষেত্রে প্রযোজ্য।",
                points = 1,
                imageUrl = null,
                order = 3
            ),
            QuestionEntity(
                id = "q_s2_4",
                quizId = "quiz_c9_s2_q1",
                type = QuestionType.MCQ.name.lowercase(),
                questionText = "অভিকর্ষজ ত্বরণ 'g' এর আদর্শ মান কত?",
                options = listOf("9.8 m/s²", "9.81 m/s²", "9.78 m/s²", "10 m/s²"),
                answer = "9.8 m/s²",
                acceptedAnswers = listOf("9.8 m/s²", "9.8", "9.81"),
                explanation = "ভূপৃষ্ঠে অভিকর্ষজ ত্বরণের সার্বজনীন আদর্শ মান ৯.৮ মিটার/সেকেন্ড² হিসেবে ধরা হয়।",
                points = 1,
                imageUrl = null,
                order = 4
            )
        )
    }

    fun getInitialAnnouncements(): List<AnnouncementEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            AnnouncementEntity(
                id = "ann_1",
                title = "মেধাকুইজে স্বাগতম!",
                message = "১ম থেকে ১০ম শ্রেণির সকল বিষয়ের অধ্যায়ভিত্তিক কুইজ এবং এআই সহায়তায় প্রস্তুতি শুরু করুন।",
                date = "২৭ সেপ্টেম্বর ২০২৬",
                isImportant = true,
                isActive = true,
                createdAt = now
            ),
            AnnouncementEntity(
                id = "ann_2",
                title = "নতুন কুইজ সংযোজিত হয়েছে",
                message = "৯ম ও ১০ম শ্রেণির গণিত এবং বিজ্ঞানের নতুন প্র্যাকটিস ও এক্সাম কুইজ উন্মুক্ত করা হয়েছে।",
                date = "২৬ সেপ্টেম্বর ২০২৬",
                isImportant = false,
                isActive = true,
                createdAt = now - 86400000
            )
        )
    }
}
