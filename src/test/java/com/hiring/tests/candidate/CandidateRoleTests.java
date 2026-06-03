package com.hiring.tests.candidate;

import com.hiring.commonMethods.CommonMethod;
import com.hiring.helpers.ActorHelper;
import com.hiring.utils.BaseTest;
import com.hiring.utils.RestUtils;
import com.hiring.utils.UserTokenManager;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CandidateRoleTests — Three focused tests for Candidate role
 *
 * Actors:
 *  recruiter1 → Recruiter1  (userDetails prefix: recruiter1)
 *  candidate1 → Candidate1  (userDetails prefix: candidate1)
 *
 * Test cases:
 *  TC_CR_001 – Candidate Views All Job Listings
 *  TC_CR_002 – Candidate Applies for a Job
 *  TC_CR_003 – Candidate Tracks Application Status
 */
public class CandidateRoleTests extends BaseTest {

    private static final Logger log = LogManager.getLogger(CandidateRoleTests.class);

    // ── UserTokenManager — single source of truth for all credentials ──────────
    private UserTokenManager tokenManager;

    // ── Actors: only the roles this test class needs ──────────────────────────
    public ActorHelper actorHelperForRecruiter1;
    public ActorHelper actorHelperForCandidate1;

    public RestUtils restUtilsForRecruiter1;
    public RestUtils restUtilsForCandidate1;

    // ═══════════════════════════════════════════════════════════════════════════
    //  SETUP — runs once before all @Test methods
    // ═══════════════════════════════════════════════════════════════════════════

    @BeforeClass
    public void setUp() {
        /*
         * UserTokenManager reads userDetails.properties and generates all tokens.
         * NEVER hardcode email or password — always use tokenManager.getToken/getEmail/getPassword.
         */
        tokenManager = new UserTokenManager();

        restUtilsForRecruiter1 = tokenManager.getRestUtils("recruiter1");
        restUtilsForCandidate1 = tokenManager.getRestUtils("candidate1");

        actorHelperForRecruiter1 = tokenManager.getActorHelper("recruiter1");
        actorHelperForCandidate1 = tokenManager.getActorHelper("candidate1");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TC_CR_001 — Candidate Views All Job Listings
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_CR_001 — Candidate Views All Job Listings
     * Verifies that a Candidate can retrieve all active job listings and that each
     * job contains required fields: id, title, company, location, type, active=true.
     */
    @Test(groups = {"Smoke"}, description = "TC_CR_001 - Candidate Views All Job Listings")
    public void candidateViewsAllJobListings() throws Exception {

        HashMap<String, String> testData = CommonMethod.readTestData(
                "src/main/resources/testdata/candidateViewsAllJobListings.json");

        // ── STEP 01 — Recruiter1 creates a job to seed at least one active listing ──
        log.info("[E2E] ━━━ STEP 01 ━━━ Recruiter1 creates a job to ensure at least one active listing exists");
        HashMap<String, String> jobData = new HashMap<>();
        jobData.put("title",       testData.get("job1.title"));
        jobData.put("description", testData.get("job1.description"));
        jobData.put("location",    testData.get("job1.location"));
        jobData.put("company",     testData.get("job1.company"));
        jobData.put("salary",      testData.get("job1.salary"));
        jobData.put("type",        testData.get("job1.type"));
        Response createJobResp = actorHelperForRecruiter1.createJob(jobData);
        Assert.assertEquals(createJobResp.getStatusCode(), 200,
                "Recruiter1 job creation failed — expected HTTP 200");
        String seededJobId = createJobResp.jsonPath().getString("data.id");
        Assert.assertNotNull(seededJobId, "Seeded job ID must not be null");
        Assert.assertTrue(createJobResp.jsonPath().getBoolean("data.active"),
                "Newly created job must be active");
        log.info("[E2E] Seeded job ID={}", seededJobId);

        // ── STEP 02 — Candidate1 retrieves all active job listings ──────────
        log.info("[E2E] ━━━ STEP 02 ━━━ Candidate1 retrieves all active job listings: GET /api/jobs");
        Response allJobsResp = actorHelperForCandidate1.getAllJobs();
        Assert.assertEquals(allJobsResp.getStatusCode(), 200,
                "Get all jobs failed — expected HTTP 200");

        // ── STEP 03 — Validate response structure and each job's required fields ──
        log.info("[E2E] ━━━ STEP 03 ━━━ Validate response structure and job field completeness");
        Assert.assertTrue(allJobsResp.jsonPath().getBoolean("success"),
                "Response 'success' flag must be true");

        List<Map<String, Object>> jobs = allJobsResp.jsonPath().getList("data");
        Assert.assertNotNull(jobs, "Job data list must not be null");
        Assert.assertTrue(jobs.size() >= 1,
                "Job listing must contain at least one job (the seeded job must be visible)");

        // Verify every job in the list has all required fields and is active;
        // also confirm the seeded job appears with correct data
        boolean seededJobFound = false;
        for (Map<String, Object> job : jobs) {
            Assert.assertNotNull(job.get("id"),       "Each job must have an 'id' field");
            Assert.assertNotNull(job.get("title"),    "Each job must have a 'title' field");
            Assert.assertNotNull(job.get("company"),  "Each job must have a 'company' field");
            Assert.assertNotNull(job.get("location"), "Each job must have a 'location' field");
            Assert.assertNotNull(job.get("type"),     "Each job must have a 'type' field");
            Assert.assertTrue((Boolean) job.get("active"),
                    "All listed jobs must have active=true (inactive jobs must not appear)");

            if (seededJobId.equals(String.valueOf(job.get("id")))) {
                Assert.assertEquals(job.get("title"),    testData.get("job1.title"),
                        "Seeded job title must match");
                Assert.assertEquals(job.get("company"),  testData.get("job1.company"),
                        "Seeded job company must match");
                Assert.assertEquals(job.get("location"), testData.get("job1.location"),
                        "Seeded job location must match");
                Assert.assertEquals(job.get("type"),     testData.get("job1.type"),
                        "Seeded job type must match");
                seededJobFound = true;
            }
        }
        Assert.assertTrue(seededJobFound,
                "The seeded job (ID=" + seededJobId + ") must be present in the active listings");
        log.info("[E2E] Job listing validated — {} active jobs found, seeded job confirmed", jobs.size());

        log.info("[E2E] ━━━ ALL STEPS COMPLETED SUCCESSFULLY ━━━");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TC_CR_002 — Candidate Applies for a Job
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_CR_002 — Candidate Applies for a Job
     * Verifies that a Candidate can successfully apply for an active job. Asserts that
     * the application is created with status PENDING, the response contains the correct
     * job ID, cover letter, candidate role, and an appliedAt timestamp.
     */
    @Test(groups = {"Regression"}, description = "TC_CR_002 - Candidate Applies for a Job")
    public void candidateAppliesForJob() throws Exception {

        HashMap<String, String> testData = CommonMethod.readTestData(
                "src/main/resources/testdata/candidateAppliesForJob.json");

        // ── STEP 01 — Recruiter1 creates a job to obtain a valid jobId ────────
        log.info("[E2E] ━━━ STEP 01 ━━━ Recruiter1 creates a new job: POST /api/jobs");
        HashMap<String, String> jobData = new HashMap<>();
        jobData.put("title",       testData.get("job1.title"));
        jobData.put("description", testData.get("job1.description"));
        jobData.put("location",    testData.get("job1.location"));
        jobData.put("company",     testData.get("job1.company"));
        jobData.put("salary",      testData.get("job1.salary"));
        jobData.put("type",        testData.get("job1.type"));
        Response createJobResp = actorHelperForRecruiter1.createJob(jobData);
        Assert.assertEquals(createJobResp.getStatusCode(), 200,
                "Recruiter1 job creation failed — expected HTTP 200");
        String jobId = createJobResp.jsonPath().getString("data.id");
        Assert.assertNotNull(jobId, "Job ID must not be null before applying");
        log.info("[E2E] Created job ID={}", jobId);

        // ── STEP 02 — Candidate1 submits a job application ────────────────────
        log.info("[E2E] ━━━ STEP 02 ━━━ Candidate1 submits application: POST /api/applications");
        HashMap<String, String> applyData = new HashMap<>();
        applyData.put("jobId",       jobId);
        applyData.put("coverLetter", testData.get("apply.coverLetter"));
        Response applyResp = actorHelperForCandidate1.applyForJob(applyData);
        Assert.assertEquals(applyResp.getStatusCode(), 200,
                "Application submission failed — expected HTTP 200");

        // ── STEP 03 — Validate all fields in the application response ─────────
        log.info("[E2E] ━━━ STEP 03 ━━━ Validate application response fields");

        String applicationId = applyResp.jsonPath().getString("data.id");
        Assert.assertNotNull(applicationId,
                "Application ID must not be null in the response");

        Assert.assertEquals(applyResp.jsonPath().getString("data.status"), "PENDING",
                "Application status must be PENDING immediately after submission");

        Assert.assertEquals(applyResp.jsonPath().getString("data.job.id"), jobId,
                "data.job.id in response must match the job applied to");

        Assert.assertEquals(applyResp.jsonPath().getString("data.coverLetter"),
                testData.get("apply.coverLetter"),
                "Returned cover letter must exactly match the submitted cover letter");

        Assert.assertNotNull(applyResp.jsonPath().getString("data.appliedAt"),
                "appliedAt timestamp must be present in the application response");

        Assert.assertTrue(applyResp.jsonPath().getBoolean("data.job.active"),
                "The applied job must still be active after application submission");

        Assert.assertEquals(applyResp.jsonPath().getString("data.candidate.role"), "CANDIDATE",
                "Candidate role in response must be CANDIDATE");

        Assert.assertNotNull(applyResp.jsonPath().getString("data.candidate.email"),
                "Candidate email must be present in application response");

        log.info("[E2E] Application ID={} confirmed — status=PENDING, coverLetter and job.id validated",
                applicationId);

        log.info("[E2E] ━━━ ALL STEPS COMPLETED SUCCESSFULLY ━━━");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TC_CR_003 — Candidate Tracks Application Status
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_CR_003 — Candidate Tracks Application Status
     * Verifies that a Candidate can retrieve their own applications after submitting one,
     * and that the tracked application shows the correct status (PENDING), cover letter,
     * job reference (id, title, company, location), and an appliedAt timestamp.
     */
    @Test(groups = {"Regression"}, description = "TC_CR_003 - Candidate Tracks Application Status")
    public void candidateTracksApplicationStatus() throws Exception {

        HashMap<String, String> testData = CommonMethod.readTestData(
                "src/main/resources/testdata/candidateTracksApplicationStatus.json");

        // ── STEP 01 — Recruiter1 creates a new job ─────────────────────────────
        log.info("[E2E] ━━━ STEP 01 ━━━ Recruiter1 creates a new job: POST /api/jobs");
        HashMap<String, String> jobData = new HashMap<>();
        jobData.put("title",       testData.get("job1.title"));
        jobData.put("description", testData.get("job1.description"));
        jobData.put("location",    testData.get("job1.location"));
        jobData.put("company",     testData.get("job1.company"));
        jobData.put("salary",      testData.get("job1.salary"));
        jobData.put("type",        testData.get("job1.type"));
        Response createJobResp = actorHelperForRecruiter1.createJob(jobData);
        Assert.assertEquals(createJobResp.getStatusCode(), 200,
                "Recruiter1 job creation failed — expected HTTP 200");
        String jobId = createJobResp.jsonPath().getString("data.id");
        Assert.assertNotNull(jobId, "Job ID must not be null");
        log.info("[E2E] Created job ID={}", jobId);

        // ── STEP 02 — Candidate1 applies for the job ──────────────────────────
        log.info("[E2E] ━━━ STEP 02 ━━━ Candidate1 applies for the job: POST /api/applications");
        HashMap<String, String> applyData = new HashMap<>();
        applyData.put("jobId",       jobId);
        applyData.put("coverLetter", testData.get("apply.coverLetter"));
        Response applyResp = actorHelperForCandidate1.applyForJob(applyData);
        Assert.assertEquals(applyResp.getStatusCode(), 200,
                "Application submission failed — expected HTTP 200");
        String applicationId = applyResp.jsonPath().getString("data.id");
        Assert.assertNotNull(applicationId, "Application ID must not be null");
        log.info("[E2E] Submitted application ID={}", applicationId);

        // ── STEP 03 — Candidate1 retrieves their application list (dashboard) ──
        log.info("[E2E] ━━━ STEP 03 ━━━ Candidate1 retrieves application list: GET /api/applications/my");
        Response myAppsResp = actorHelperForCandidate1.getMyApplications();
        Assert.assertEquals(myAppsResp.getStatusCode(), 200,
                "Get my applications failed — expected HTTP 200");

        // ── STEP 04 — Validate the tracked application in the dashboard ───────
        log.info("[E2E] ━━━ STEP 04 ━━━ Validate tracked application in dashboard");
        Assert.assertTrue(myAppsResp.jsonPath().getBoolean("success"),
                "Response 'success' flag must be true");

        List<Map<String, Object>> myApps = myAppsResp.jsonPath().getList("data");
        Assert.assertNotNull(myApps, "Applications list must not be null");
        Assert.assertTrue(myApps.size() >= 1,
                "Candidate1 must have at least one application in the dashboard");

        // Find the just-submitted application by its ID and validate every tracked field
        boolean appFound = false;
        for (Map<String, Object> app : myApps) {
            if (applicationId.equals(String.valueOf(app.get("id")))) {

                Assert.assertEquals(app.get("status"), "PENDING",
                        "Tracked application status must be PENDING");

                Assert.assertEquals(app.get("coverLetter"), testData.get("apply.coverLetter"),
                        "Tracked cover letter must match the submitted cover letter");

                Assert.assertNotNull(app.get("appliedAt"),
                        "appliedAt timestamp must be present in the dashboard response");

                Map<String, Object> trackedJob = (Map<String, Object>) app.get("job");
                Assert.assertNotNull(trackedJob,
                        "Tracked application must contain a job reference object");
                Assert.assertEquals(String.valueOf(trackedJob.get("id")), jobId,
                        "Tracked job ID must match the job applied to");
                Assert.assertNotNull(trackedJob.get("title"),
                        "Job title must be present in the tracked application");
                Assert.assertNotNull(trackedJob.get("company"),
                        "Job company must be present in the tracked application");
                Assert.assertNotNull(trackedJob.get("location"),
                        "Job location must be present in the tracked application");

                Map<String, Object> candidate = (Map<String, Object>) app.get("candidate");
                Assert.assertNotNull(candidate,
                        "Tracked application must contain a candidate reference object");
                Assert.assertEquals(candidate.get("role"), "CANDIDATE",
                        "Candidate role in dashboard must be CANDIDATE");

                appFound = true;
                break;
            }
        }
        Assert.assertTrue(appFound,
                "Submitted application (ID=" + applicationId + ") must appear in Candidate1 dashboard");
        log.info("[E2E] Tracked application confirmed — status=PENDING, job ID={}, all fields validated",
                jobId);

        log.info("[E2E] ━━━ ALL STEPS COMPLETED SUCCESSFULLY ━━━");
    }
}

