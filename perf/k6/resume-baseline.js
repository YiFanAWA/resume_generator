import http from "k6/http";
import { check, sleep } from "k6";
import { Trend } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:5000";
const USERNAME = __ENV.USERNAME || "alice";
const PASSWORD = __ENV.PASSWORD || "password123";
const SHARE_TOKEN = __ENV.SHARE_TOKEN || "";
const INCLUDE_PDF = (__ENV.INCLUDE_PDF || "true").toLowerCase() === "true";
const INCLUDE_WRITES = (__ENV.INCLUDE_WRITES || "true").toLowerCase() === "true";
const PDF_SAMPLE_RATE = Number(__ENV.PDF_SAMPLE_RATE || "0.2");
const READ_MAX_VUS = Number(__ENV.READ_MAX_VUS || "5");
const WRITE_VUS = Number(__ENV.WRITE_VUS || "1");

const publicResumeDuration = new Trend("public_resume_duration");
const profileDuration = new Trend("profile_duration");
const saveProfileDuration = new Trend("save_profile_duration");
const pdfExportDuration = new Trend("pdf_export_duration");

const scenarios = {
  read_baseline: {
    executor: "ramping-vus",
    exec: "readScenario",
    stages: [
      { duration: "20s", target: READ_MAX_VUS },
      { duration: "40s", target: READ_MAX_VUS },
      { duration: "20s", target: 0 }
    ],
    gracefulRampDown: "30s",
    gracefulStop: "30s"
  }
};

if (INCLUDE_WRITES) {
  scenarios.write_baseline = {
    executor: "constant-vus",
    exec: "writeScenario",
    vus: WRITE_VUS,
    duration: "1m20s",
    gracefulStop: "30s"
  };
}

export const options = {
  scenarios,
  thresholds: {
    http_req_failed: ["rate<0.01"],
    checks: ["rate>0.99"],
    http_req_duration: ["p(95)<1000"]
  }
};

export function readScenario() {
  login();
  readProfile();
  readPublicResume();
  exportPdfSample();
  sleep(1);
}

export function writeScenario() {
  login();
  saveProfile();
  sleep(1);
}

function login() {
  const response = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    {
      headers: jsonHeaders(),
      tags: { endpoint: "login" }
    }
  );

  check(response, {
    "login status is 200": (res) => res.status === 200
  });
}

function readProfile() {
  const response = http.get(`${BASE_URL}/api/profile`, {
    tags: { endpoint: "profile" }
  });
  profileDuration.add(response.timings.duration);

  check(response, {
    "profile status is 200": (res) => res.status === 200
  });
}

function readPublicResume() {
  if (!SHARE_TOKEN) {
    return;
  }

  const response = http.get(`${BASE_URL}/api/public/${SHARE_TOKEN}`, {
    tags: { endpoint: "public_resume" }
  });
  publicResumeDuration.add(response.timings.duration);

  check(response, {
    "public resume status is 200": (res) => res.status === 200
  });
}

function exportPdfSample() {
  if (!INCLUDE_PDF || Math.random() >= PDF_SAMPLE_RATE) {
    return;
  }

  const response = http.get(`${BASE_URL}/api/profile/export/pdf`, {
    tags: { endpoint: "pdf_export" }
  });
  pdfExportDuration.add(response.timings.duration);

  check(response, {
    "pdf export status is 200": (res) => res.status === 200,
    "pdf export content type": (res) =>
      (res.headers["Content-Type"] || "").includes("application/pdf")
  });
}

function saveProfile() {
  const response = http.put(
    `${BASE_URL}/api/profile`,
    JSON.stringify(profilePayload()),
    {
      headers: jsonHeaders(),
      tags: { endpoint: "save_profile" }
    }
  );
  saveProfileDuration.add(response.timings.duration);

  check(response, {
    "save profile status is 200": (res) => res.status === 200
  });
}

function jsonHeaders() {
  return { "Content-Type": "application/json" };
}

function profilePayload() {
  return {
    firstName: "Alice",
    lastName: "LoadTest",
    email: "alice@example.com",
    phone: "1234567890",
    designation: "Backend Engineer",
    summary: `Load-test profile saved by VU ${__VU}, iteration ${__ITER}.`,
    theme: 1,
    skills: ["Java", "Spring Boot", "React", "SQL"],
    jobs: [
      {
        company: "Baseline Labs",
        designation: "Engineer",
        startDate: "2024-01-01",
        endDate: null,
        currentJob: true,
        responsibilities: [
          "Build and maintain resume generation APIs.",
          "Improve PDF export and public sharing performance."
        ]
      }
    ],
    educations: [
      {
        college: "Example University",
        qualification: "Computer Science",
        startDate: "2020-09-01",
        endDate: "2024-06-01",
        summary: "Software engineering and distributed systems."
      }
    ]
  };
}
