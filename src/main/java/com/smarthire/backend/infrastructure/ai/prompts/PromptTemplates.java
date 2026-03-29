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
            Analyze this CV/Resume document and extract structured information.
            
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
}
