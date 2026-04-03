package com.smarthire.backend.infrastructure.ai.prompts;

/**
 * Prompt templates cho các chức năng AI.
 * Mỗi prompt yêu cầu Gemini trả về JSON format cố định để dễ parse.
 */
public final class PromptTemplates {

    private PromptTemplates() {}

    /**
     * CV Parse — Trích xuất thông tin cá nhân từ CV.
     * Input: File PDF được upload kèm prompt này.
     * Output: JSON chứa personal info.
     */
    public static final String CV_PARSE_PROMPT = """
            Analyze this CV/Resume text and extract structured information.
            
            === CV TEXT ===
            %s
            
            === INSTRUCTIONS ===
            
            Return ONLY a valid JSON object with this EXACT structure (no markdown, no code blocks, just raw JSON):
            {
                "firstName": "string or empty",
                "lastName": "string or empty",
                "phone": "string or empty",
                "email": "string or empty",
                "linkedin": "URL string or empty",
                "website": "URL string or empty (GitHub, portfolio, etc.)",
                "country": "string or empty",
                "state": "string or empty",
                "city": "string or empty",
                "gender": "MALE or FEMALE or OTHER or empty string",
                "summary": "Professional summary — concise 2-4 sentences MAX. Capture the candidate's role, years of experience, and key strengths. Do NOT copy long paragraphs verbatim.",
                "skills": ["skill1", "skill2"],
                "experience": [
                    {
                        "company": "Company Name",
                        "title": "Job Title",
                        "startDate": "YYYY-MM or Month Year",
                        "endDate": "YYYY-MM or Present",
                        "description": "Concise summary of key responsibilities and achievements. Keep under 3-4 sentences. Focus on impact and results, not listing every feature or module."
                    }
                ],
                "education": [
                    {
                        "school": "University/Institution name",
                        "degree": "Degree name (e.g. Bachelor, Master)",
                        "major": "Field of study",
                        "startDate": "YYYY-MM or Year",
                        "endDate": "YYYY-MM or Present"
                    }
                ]
            }
            
            SKILLS EXTRACTION RULES (VERY IMPORTANT):
            - Extract ONLY 6-10 core technical skills that best represent the candidate's expertise
            - Focus on: programming languages, major frameworks, and primary tools
            - DO NOT include: soft skills (teamwork, communication, time management, etc.)
            - DO NOT include: generic concepts (OOP, Data Structures, Algorithms, Design Patterns)
            - DO NOT include: basic dev tools everyone uses (Git, VS Code, ESLint, Terminal)
            - DO NOT include: CI/CD tools, project management tools (Jira, Figma, Docker) unless they are central to the role
            - Prioritize skills that would appear in a job listing for their role
            - Example for a Frontend Developer: ["React", "Next.js", "TypeScript", "Tailwind CSS", "Node.js", "PostgreSQL"]
            - Example for a Data Engineer: ["Python", "Apache Spark", "SQL", "Airflow", "AWS", "Kafka"]
            
            DESCRIPTION RULES:
            - Keep experience descriptions SHORT and impactful (2-4 sentences max)
            - Focus on: what the candidate built, technologies used, and measurable outcomes
            - DO NOT include: GitHub URLs, demo links, or any URLs inside descriptions
            - DO NOT list every single feature/module — summarize the overall contribution
            - DO NOT copy bullet points verbatim — synthesize into clean prose
            - Use past tense for previous roles, present tense for current role
            
            SUMMARY RULES:
            - Write a concise 2-3 sentence professional summary
            - Include: target role, experience level, and 2-3 key technical strengths
            - DO NOT copy the candidate's original summary verbatim if it's too long
            - If no summary section exists, generate one from the overall CV context
            
            OTHER RULES:
            - Extract real data from the CV, do NOT fabricate information
            - If a field cannot be found, use empty string "" (or empty array [] for lists)
            - Skill names should be in their standard English form (e.g. "React" not "ReactJS")
            - Return ONLY the JSON object, absolutely no other text
            """;

    /**
     * CV-JD Matching — So sánh CV với Job Description.
     * Input: Text prompt chứa cả CV content lẫn JD.
     * Output: JSON chứa score + breakdown + analysis.
     */
    public static final String CV_JOB_MATCH_PROMPT = """
            You are an expert HR recruiter AI. Analyze how well a candidate's CV matches a job description.
            
            === CANDIDATE CV CONTENT ===
            %s
            
            === JOB DESCRIPTION ===
            Title: %s
            Description: %s
            Requirements: %s
            Required Skills: %s
            Job Level: %s
            
            ===
            
            Return ONLY a valid JSON object with this EXACT structure (no markdown, no code blocks, just raw JSON):
            {
                "scoreTotal": 75.5,
                "scoreBreakdown": {
                    "skills_match": 80,
                    "experience_match": 70,
                    "education_match": 75,
                    "overall_fit": 77
                },
                "strengths": ["strength 1", "strength 2", "strength 3"],
                "gaps": ["gap 1", "gap 2"],
                "recommendations": ["recommendation 1", "recommendation 2"],
                "explanation": "Brief 2-3 sentence explanation of the match"
            }
            
            Rules:
            - scoreTotal must be a number between 0 and 100
            - Each score in scoreBreakdown must be between 0 and 100
            - Provide 2-5 strengths, 1-3 gaps, 1-3 recommendations
            - Be realistic and fair in scoring
            - CRITICAL: The ENTIRE JSON response fields (strengths, gaps, recommendations, explanation) MUST be written completely in Vietnamese (Tiếng Việt).
            - Return ONLY the JSON, no other text
            """;

    /**
     * CV Review — Đánh giá chất lượng CV chuyên nghiệp.
     * Input: File PDF được upload kèm system rules JSON.
     * Output: JSON chứa section-level scoring, structured items, ATS score.
     */
    public static final String CV_REVIEW_PROMPT = """
            You are an expert, professional HR Headhunter with 15+ years of experience and an ATS algorithm specialist.
            Analyze this CV document comprehensively for quality, formatting, content effectiveness, and ATS compatibility.
            
            STRICTLY follow these governance rules (provided as JSON) when evaluating:
            %s
            
            === CV TEXT ===
            %s
            
            SCORING THRESHOLD (used to decide status for each item):
            - 0-49 (WEAK/CRITICAL): Needs strong revision, may need complete rewrite
            - 50-69 (IMPROVE): Selective improvement needed, only rewrite if truly necessary
            - 70-84 (GOOD): Can keep as is, only suggest if specific issue or JD mismatch
            - 85-100 (STRONG): Do NOT change, mark as KEEP
            
            EVALUATION CRITERIA for each bullet/item:
            - Clarity: Is the meaning immediately clear?
            - Specificity: Are there concrete details (numbers, tools, outcomes)?
            - Action Verb Strength: Does it start with a strong action verb?
            - Technical Relevance: Is it relevant to the target role?
            - Measurable Impact: Does it quantify achievements?
            - Brevity: Is it concise without being vague?
            - Truth Preservation: Does it match the apparent experience level?
            
            Return ONLY a valid JSON object with this EXACT structure (no markdown, no code blocks, just raw JSON):
            {
                "overallScore": 72,
                "atsScore": 65,
                "dataCompleteness": {
                    "level": "CRITICAL|POOR|ADEQUATE|GOOD",
                    "filledSections": 3,
                    "totalExpectedSections": 7,
                    "missingCritical": ["Tóm tắt nghề nghiệp", "Kỹ năng chi tiết"],
                    "junkEntries": ["aaaa", "123", "test"],
                    "verdict": "Brief Vietnamese verdict",
                    "canOptimize": false
                },
                "sections": [
                    {
                        "name": "Section name",
                        "score": 75,
                        "status": "STRONG|GOOD|IMPROVE|WEAK",
                        "items": [
                            {
                                "type": "BULLET|SECTION|FIELD",
                                "originalText": "The exact text from the CV verbatim",
                                "status": "STRONG|GOOD|IMPROVE|WEAK|CRITICAL",
                                "reason": "Specific reason in Vietnamese",
                                "action": "KEEP|IMPROVE|REWRITE|MUST_FIX",
                                "priority": "HIGH|MEDIUM|LOW|NONE",
                                "confidence": "HIGH|MEDIUM|LOW",
                                "suggestedText": "Improved version or null if KEEP"
                            }
                        ]
                    }
                ],
                "topIssues": [
                    {
                        "section": "Section name",
                        "severity": "HIGH|MEDIUM|LOW",
                        "category": "ATS|CONTENT|FORMAT|KEYWORD|GRAMMAR|STRUCTURE|MISSING_INFO",
                        "description": "Issue description in Vietnamese",
                        "quote": "Exact text from CV or N/A if missing section"
                    }
                ],
                "strengths": ["Strength in Vietnamese"],
                "weaknesses": ["Weakness in Vietnamese"],
                "summary": "Brief 2-3 sentence overall assessment in Vietnamese"
            }
            
            CRITICAL RULES:
            - overallScore and atsScore must be integers 0-100
            - Each section score must be integer 0-100
            - Provide ALL sections found in the CV (Summary, Experience, Education, Skills, Projects, Contact, etc.)
            - For each section, evaluate EVERY bullet point or key field as an item
            - Items with status=STRONG MUST have action=KEEP and suggestedText=null
            - Items with action=KEEP MUST NOT have suggestedText (set to null)
            - suggestedText must preserve original meaning — NEVER fabricate data
            - topIssues: exactly 3-5 issues, sorted by priority (ATS > Missing Info > Weak Content > Keywords > Polish)
            - Provide 2-5 strengths and 2-5 weaknesses
            - originalText in items MUST be exact words from the CV verbatim (do NOT paraphrase)
            - All Vietnamese text for: reason, description, summary, strengths, weaknesses, verdict
            - originalText and quote: keep in original CV language exactly as written
            
            IMPORTANT — ORIGINALTEXT ALIGNMENT:
            - The originalText for each item MUST be a substring that can be found in the CV Builder's structured data fields
            - For experience items: use the text from individual description fields, NOT the entire experience block
            - For skills items: use individual skill names, NOT a comma-separated list
            - For summary: use the summary text as a whole
            - Keep originalText short and precise — it should match a single field value in the CV data structure
            - DO NOT combine multiple fields into one originalText
            
            DATA COMPLETENESS DETECTION (VERY IMPORTANT):
            - CRITICAL: CV has placeholder/junk data ("aaaa", "test", "123", single letters, random strings) OR is missing 4+ critical sections. canOptimize=false.
            - POOR: CV has real data but missing 2-3 important sections (e.g. no skills, no summary). canOptimize=false.
            - ADEQUATE: CV has most sections filled with real content, minor gaps. canOptimize=true.
            - GOOD: CV is complete with meaningful content in all sections. canOptimize=true.
            - junkEntries: list ALL entries that look like placeholder/test data
            - missingCritical: list sections that are completely missing or empty
            - If level=CRITICAL or POOR, the verdict MUST clearly tell the user to go back and fill in real information
            
            - Return ONLY the JSON, absolutely no conversational text or markdown
            """;

    /**
     * CV Optimize — Tối ưu toàn bộ CV theo review.
     * Input: File PDF + review results JSON.
     * Output: JSON chứa optimized sections với suggestedText cho mọi item cần cải thiện.
     */
    public static final String CV_OPTIMIZE_PROMPT = """
            You are an expert CV optimizer. You have already reviewed this CV.
            Below is the review result with section-by-section analysis.
            
            PREVIOUS REVIEW RESULT:
            %s
            
            === CV TEXT ===
            %s
            
            YOUR TASK:
            Generate an OPTIMIZED version for every item that has action != "KEEP".
            For items with action = "KEEP", keep them exactly as-is.
            
            OPTIMIZATION RULES (STRICT):
            1. Preserve original meaning — NEVER fabricate data, numbers, or achievements
            2. Minimal necessary change — don't rewrite if a small edit suffices
            3. Keep the candidate's voice and style
            4. Do NOT inflate role titles or responsibilities
            5. Add quantifiable metrics ONLY if they can be reasonably inferred
            6. Make bullet points concise, action-verb-led, and ATS-friendly
            7. Fix formatting issues (inconsistent tense, missing periods, etc.)
            8. Validate: each rewrite must be genuinely better than original
            9. Keep descriptions SHORT — 2-4 sentences max per experience entry
            10. Remove any URLs, links, or references from optimized text
            
            Return ONLY a valid JSON object with this EXACT structure:
            {
                "optimizedSections": [
                    {
                        "sectionName": "Section name matching the review",
                        "items": [
                            {
                                "originalText": "Exact original text from the review's originalText field",
                                "optimizedText": "Improved version (or same text if KEEP)",
                                "action": "KEEP|IMPROVED|REWRITTEN",
                                "changeReason": "Brief reason for the change in Vietnamese (or 'Giữ nguyên' if KEEP)"
                            }
                        ]
                    }
                ],
                "optimizedSummary": "Rewritten professional summary — concise 2-3 sentences. Include role, experience level, and key strengths.",
                "changeCount": 5,
                "preservedCount": 12
            }
            
            CRITICAL:
            - originalText MUST match EXACTLY the originalText from the review items (character by character)
            - This is used for text replacement in the CV Builder — any mismatch will cause the optimization to fail silently
            - optimizedText for KEEP items = same as originalText
            - optimizedText should be clean prose, no URLs or markdown
            - changeReason must be in Vietnamese
            - Return ONLY the JSON, no other text
            """;

    // ═══════════════════════════════════════════════════════════════
    //  SMART CV FILTERING — 2-Phase AI prompts for HR batch filtering
    // ═══════════════════════════════════════════════════════════════

    /**
     * Phase 1 — Batch Pre-filter.
     * AI reads ONLY title + summary of each candidate to quickly classify them.
     * Input: JD text + JSON array of candidate snippets.
     * Output: JSON array of classification results.
     *
     * Placeholders:
     *   %s → Job title
     *   %s → Job description
     *   %s → Job requirements
     *   %s → Required skills (comma-separated)
     *   %s → Job level
     *   %s → HR filter conditions JSON
     *   %s → Candidate snippets JSON array
     */
    public static final String CV_BATCH_PRE_FILTER_PROMPT = """
            You are a senior HR recruiter AI performing fast CV screening.
            Your task: quickly classify candidates by reading ONLY their CV title and professional summary.
            DO NOT evaluate in depth — this is a quick pre-screening step.
            
            === JOB DESCRIPTION ===
            Title: %s
            Description: %s
            Requirements: %s
            Required Skills: %s
            Job Level: %s
            
            === HR FILTER CONDITIONS ===
            %s
            
            === CANDIDATE SNIPPETS ===
            %s
            
            === INSTRUCTIONS ===
            For each candidate, determine if they are potentially suitable for this position based
            ONLY on their title, summary, current role, and top keywords.
            
            Return ONLY a valid JSON array (no markdown, no code blocks, just raw JSON):
            [
                {
                    "candidateId": 123,
                    "classification": "SUITABLE | NOT_SUITABLE | NEEDS_REVIEW",
                    "confidence": "HIGH | MEDIUM | LOW",
                    "reason": "Brief reason in Vietnamese (1-2 sentences max)"
                }
            ]
            
            CLASSIFICATION RULES:
            - SUITABLE: Title/summary clearly aligns with the job role and requirements
            - NOT_SUITABLE: Title/summary indicates a completely different career path or skill set
            - NEEDS_REVIEW: Ambiguous — could be suitable but need deeper analysis to confirm
            - When in doubt, prefer NEEDS_REVIEW over NOT_SUITABLE
            - If candidate has no title or summary, classify as NEEDS_REVIEW
            - Do NOT reject candidates just because of seniority mismatch — a junior can apply for mid-level
            - Focus on ROLE ALIGNMENT, not perfection
            
            CRITICAL:
            - You MUST return results for ALL candidates in the input array
            - candidateId must match exactly from the input
            - reason must be in Vietnamese
            - Return ONLY the JSON array, no other text
            """;

    /**
     * Phase 2 — Deep Evaluation.
     * AI reads the FULL CV content and performs detailed matching against JD.
     * Run only for candidates that passed Phase 1 pre-filter.
     *
     * Placeholders:
     *   %s → Job title
     *   %s → Job description
     *   %s → Job requirements
     *   %s → Required skills (comma-separated)
     *   %s → Job level
     *   %s → HR filter conditions JSON
     *   %s → Full CV content text
     */
    public static final String CV_DEEP_EVALUATION_PROMPT = """
            You are a senior HR recruiter AI performing detailed CV evaluation.
            This candidate has already passed the initial screening and is potentially suitable.
            Now perform a thorough analysis.
            
            === JOB DESCRIPTION ===
            Title: %s
            Description: %s
            Requirements: %s
            Required Skills: %s
            Job Level: %s
            
            === HR FILTER CONDITIONS ===
            %s
            
            === FULL CV CONTENT ===
            %s
            
            === INSTRUCTIONS ===
            Evaluate this candidate thoroughly against the job description.
            Consider: skills match, experience relevance, education, projects, certifications,
            and overall career trajectory.
            
            Return ONLY a valid JSON object (no markdown, no code blocks, just raw JSON):
            {
                "matchScore": 78,
                "classification": "STRONG_FIT | MODERATE_FIT | WEAK_FIT | NOT_SUITABLE",
                "confidence": "HIGH | MEDIUM | LOW",
                "summary": "2-3 sentence assessment in Vietnamese",
                "strengths": ["strength 1 in Vietnamese", "strength 2"],
                "missingRequirements": ["missing 1 in Vietnamese", "missing 2"],
                "recommendation": "Hiring recommendation in Vietnamese (1-2 sentences)"
            }
            
            SCORING GUIDE:
            - 85-100 (STRONG_FIT): Excellent match. Meets most/all requirements. Ready for interview.
            - 60-84 (MODERATE_FIT): Good potential. Meets core requirements but has gaps.
            - 30-59 (WEAK_FIT): Partial match. Significant gaps but some transferable skills.
            - 0-29 (NOT_SUITABLE): Does not meet key requirements.
            
            EVALUATION RULES:
            - matchScore must be an integer 0-100
            - DO NOT score based only on keyword matching — evaluate context and relevance
            - Consider years of experience, project complexity, and role alignment
            - Provide 2-5 strengths, 1-4 missingRequirements
            - If the candidate exceeds requirements, score accordingly (90+)
            - All text fields must be in Vietnamese
            - Return ONLY the JSON, no other text
            """;

    /**
     * M3.4 Generate Interview Questions — Tạo câu hỏi phỏng vấn dựa trên CV và JD.
     */
    public static final String GENERATE_INTERVIEW_QUESTIONS_PROMPT = """
            You are an expert HR Interviewer. Based on the candidate's CV and the Job Description,
            generate a set of personalized interview questions to assess their technical skills,
            experience gaps, and overall fit.
            
            === CANDIDATE CV CONTENT ===
            %s
            
            === JOB DESCRIPTION ===
            Title: %s
            Description: %s
            Requirements: %s
            Required Skills: %s
            Job Level: %s
            
            Return ONLY a valid JSON object with this EXACT structure (no markdown, no code blocks):
            {
                "questions": [
                    {
                        "category": "TECHNICAL | BEHAVIORAL | EXPERIENCE | SCENARIO",
                        "question": "The interview question in Vietnamese",
                        "intent": "What this question aims to evaluate (in Vietnamese)",
                        "expectedPoints": ["point 1 to listen for", "point 2"]
                    }
                ],
                "summary": "Brief explanation of the interview strategy in Vietnamese"
            }
            
            CRITICAL RULES:
            - Generate exactly 5-8 highly relevant questions.
            - Focus on the intersection of the candidate's past experience and the JD's requirements.
            - ALL textual content (question, intent, expected points, summary) MUST be in Vietnamese.
            - Return ONLY the JSON, absolutely no conversational text.
            """;

    /**
     * M3.5 Virtual Interview Evaluation — Đánh giá câu trả lời phỏng vấn của ứng viên.
     */
    public static final String VIRTUAL_INTERVIEW_EVALUATION_PROMPT = """
            You are an expert technical interviewer evaluating a candidate's answer.
            
            === INTERVIEW CONTEXT ===
            Job Title: %s
            Question Asked: %s
            Candidate's Answer: %s
            
            Return ONLY a valid JSON object with this EXACT structure (no markdown, no code blocks):
            {
                "score": 85,
                "feedback": "Detailed feedback on the answer in Vietnamese",
                "strengths": ["string", "string"],
                "weaknesses": ["string"],
                "followUpQuestion": "A natural follow-up question in Vietnamese, or null if to move on"
            }
            
            CRITICAL RULES:
            - score must be an integer from 0 to 100.
            - Evaluate based on technical accuracy, clarity, and relevance.
            - ALL textual content (feedback, strengths, weaknesses, followUpQuestion) MUST be in Vietnamese.
            - Return ONLY the JSON, absolutely no conversational text.
=======
            Rules for generating the response:
            - overallRating must be a number between 0 and 10
            - Identify 2-5 strengths and 2-5 weaknesses.
            - Provide 3-8 issues and 3-6 suggestions
            - 'quote' in issues MUST be exact words from the CV. Do NOT paraphrase it. If the issue is a missing section (e.g., no email), quote can be "N/A" or empty string.
            - CRITICAL: All textual output in the JSON (description, section, suggestion, strengths, weaknesses, summary) MUST be written in 100% Vietnamese (Tiếng Việt). Exception: The 'quote' field MUST remain in the original language of the CV exactly as written.
            - Return ONLY the JSON, absolutely no conversational text, no JSON markdown blocks.
>>>>>>> Stashed changes
            """;

    /**
     * ID Verification — Kiểm duyệt và trích xuất thông tin từ ảnh CMND/CCCD.
     */
    public static final String ONBOARDING_ID_VERIFY_PROMPT = """
            You are a strict security and KYC verification expert for Vietnam ID cards (CMND/CCCD).
            Please analyze the attached image.
            
            Return ONLY a valid JSON object with this EXACT structure (no markdown, no code blocks):
            {
                "isValid": true,
                "feedbackReason": "If isValid is false, explain why (e.g. 'Ảnh bị mờ', 'Không phải thẻ CCCD', 'Phát hiện chụp từ màn hình', 'Có chứa nội dung 18+'). If true, leave empty",
                "extractedName": "Họ và Tên trích xuất từ thẻ (nếu có)",
                "extractedIdNumber": "Số CCCD/CMND (nếu có)",
                "extractedDob": "Ngày sinh (nếu có)"
            }
            
            Rules:
            - Check for inappropriate content (NSFW/Violence). If found, reject immediately (isValid = false).
            - Check if the document appears to be a legitimate Vietnam ID document.
            - Check for major signs of forgery or taking a photo of a screen (moiré patterns).
            - Extract the data accurately inside the extracted fields. 
            - feedbackReason MUST be in Vietnamese.
            - Return ONLY the JSON object.
            """;
}
