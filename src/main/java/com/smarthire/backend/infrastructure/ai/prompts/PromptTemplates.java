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
            Analyze this CV/Resume document and extract complete information.
            
            Return ONLY a valid JSON object with this EXACT structure (no markdown, no code blocks, just raw JSON):
            {
                "firstName": "string or empty",
                "lastName": "string or empty",
                "phone": "string or empty",
                "email": "string or empty",
                "linkedin": "URL string or empty",
                "website": "URL string or empty",
                "country": "string or empty",
                "state": "string or empty",
                "city": "string or empty",
                "gender": "MALE or FEMALE or OTHER or empty string",
                "summary": "Full professional summary or objective statement from CV. Extract exactly as written if possible, or summarize if very long",
                "skills": ["skill1", "skill2"],
                "experience": [
                    {
                        "company": "Company Name",
                        "title": "Job Title",
                        "startDate": "YYYY-MM or Month Year",
                        "endDate": "YYYY-MM or Present",
                        "description": "Job description or bullet points combined into a readable paragraph. Extract actual responsibilities."
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
            
            Rules:
            - Extract real data from the CV, do NOT make up information
            - If a field cannot be found, use empty string "" (or empty array [] for lists)
            - Keep descriptions informative but concise.
            - IMPORTANT: All extracted text values (like summary, descriptions) should be returned in Vietnamese (Tiếng Việt) unless it's a proper noun (like company names, technologies, skills).
            - Return ONLY the JSON object, absolutely no other text, markdown, or code block markers.
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
     * CV Review — Đánh giá chất lượng CV với các tiêu chuẩn chuyên nghiệp.
     * Input: File PDF được upload kèm system rules.
     * Output: JSON chứa issues, suggestions, strengths, weaknesses, rating.
     */
    public static final String CV_REVIEW_PROMPT = """
            You are an expert, professional HR Headhunter and ATS algorithm specialist. Analyze this CV document for quality, formatting, content, and effectiveness.
            
            Strictly follow these professional CV judging rules (provided as JSON) to evaluate the CV:
            %s
            
            Return ONLY a valid JSON object with this EXACT structure (no markdown, no code blocks, just raw JSON):
            {
                "issues": [
                    {
                        "category": "FORMAT|CONTENT|KEYWORD|GRAMMAR|STRUCTURE", 
                        "severity": "HIGH|MEDIUM|LOW", 
                        "description": "Issue description based on the professional rules.",
                        "quote": "The exact sentence or phrase verbatim from the CV that has the issue to highlight"
                    }
                ],
                "suggestions": [
                    {"section": "Section name (e.g. Summary, Experience, Skills)", "suggestion": "Specific actionable suggestion"}
                ],
                "strengths": ["Strength 1", "Strength 2"],
                "weaknesses": ["Weakness 1", "Weakness 2"],
                "overallRating": 7.5,
                "summary": "Brief 2-3 sentence overall assessment of the CV"
            }
            
            Rules for generating the response:
            - overallRating must be a number between 0 and 10
            - Identify 2-5 strengths and 2-5 weaknesses.
            - Provide 3-8 issues and 3-6 suggestions
            - 'quote' in issues MUST be exact words from the CV. Do NOT paraphrase it. If the issue is a missing section (e.g., no email), quote can be "N/A" or empty string.
            - CRITICAL: All textual output in the JSON (description, section, suggestion, strengths, weaknesses, summary) MUST be written in 100%% Vietnamese (Tiếng Việt). Exception: The 'quote' field MUST remain in the original language of the CV exactly as written.
            - Return ONLY the JSON, absolutely no conversational text, no JSON markdown blocks.
            """;
}
