import { useEffect, useState } from "react";
import {
  generateShareLink,
  getProfile,
  getPublicProfile,
  login,
  logout,
  me,
  register,
  revokeShareLink,
  saveProfile,
  updateShareSettings
} from "./api";

const BASE_PATH = import.meta.env.BASE_URL.replace(/\/$/, "");

const emptyProfile = {
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  designation: "",
  summary: "",
  theme: 1,
  jobs: [],
  educations: [],
  skills: [],
  isPublic: false,
  shareToken: null,
  shareUrl: null,
  shareExpiresAt: null,
  shareMaxViews: null,
  shareViewCount: 0,
  shareLastViewedAt: null,
  sharePasswordProtected: false,
  shareExpired: false,
  shareLimitReached: false
};

function routeFromLocation() {
  let path = window.location.pathname;
  if (BASE_PATH && path.startsWith(BASE_PATH)) {
    path = path.slice(BASE_PATH.length) || "/";
  }
  return path.replace(/\/$/, "") || "/";
}

function navigate(path, options = {}) {
  const url = `${BASE_PATH}${path}`;
  const method = options.replace ? "replaceState" : "pushState";
  window.history[method]({}, "", url);
  window.dispatchEvent(new Event("popstate"));
}

export default function App() {
  const [route, setRoute] = useState(routeFromLocation);
  const [currentUser, setCurrentUser] = useState(null);
  const [booting, setBooting] = useState(true);
  const [notice, setNotice] = useState(null);

  useEffect(() => {
    const onRouteChange = () => setRoute(routeFromLocation());
    window.addEventListener("popstate", onRouteChange);
    return () => window.removeEventListener("popstate", onRouteChange);
  }, []);

  useEffect(() => {
    let active = true;
    me()
      .then((session) => {
        if (active && session.authenticated) {
          setCurrentUser(session.username);
        }
      })
      .catch(() => {
        if (active) {
          setCurrentUser(null);
        }
      })
      .finally(() => {
        if (active) {
          setBooting(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (booting) {
      return;
    }

    if (route === "/") {
      navigate(currentUser ? "/resume" : "/login", { replace: true });
      return;
    }

    if (route === "/resume" && !currentUser) {
      navigate("/login", { replace: true });
      return;
    }

    if ((route === "/login" || route === "/register") && currentUser) {
      navigate("/resume", { replace: true });
    }
  }, [booting, currentUser, route]);

  useEffect(() => {
    if (!notice) {
      return undefined;
    }
    const timer = window.setTimeout(() => setNotice(null), 3400);
    return () => window.clearTimeout(timer);
  }, [notice]);

  const showNotice = (message, tone = "success") => setNotice({ message, tone });

  const handleLogout = async () => {
    await logout().catch(() => null);
    setCurrentUser(null);
    navigate("/login", { replace: true });
  };

  if (booting || route === "/") {
    return <Splash />;
  }

  return (
    <>
      {notice && <Notice notice={notice} />}
      {route === "/register" ? (
        <AuthPage mode="register" onRegistered={showNotice} />
      ) : route === "/public-share" ? (
        <PublicSharePage />
      ) : route === "/resume" && currentUser ? (
        <Dashboard username={currentUser} onLogout={handleLogout} showNotice={showNotice} />
      ) : (
        <AuthPage mode="login" onLoggedIn={setCurrentUser} />
      )}
    </>
  );
}

function Splash() {
  return (
    <main className="splash">
      <div className="orb" />
      <p>Preparing your workspace</p>
    </main>
  );
}

function Notice({ notice }) {
  return <div className={`notice ${notice.tone}`}>{notice.message}</div>;
}

function AuthPage({ mode, onLoggedIn, onRegistered }) {
  const isRegister = mode === "register";
  const [form, setForm] = useState({ username: "", password: "", confirmPassword: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError("");

    try {
      if (isRegister) {
        await register(form);
        onRegistered?.("Registration complete. You can sign in now.");
        navigate("/login", { replace: true });
      } else {
        const session = await login(form);
        onLoggedIn(session.username);
        navigate("/resume", { replace: true });
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="auth-shell">
      <section className="auth-copy">
        <p className="eyebrow">Resume Portal</p>
        <h1>Build a sharper resume without wrestling the backend.</h1>
        <p>
          React now owns the interface. Spring Boot keeps the API, sessions, PDF export,
          and share links focused behind `/api/**`.
        </p>
      </section>

      <section className="auth-panel">
        <h2>{isRegister ? "Create account" : "Welcome back"}</h2>
        <p>{isRegister ? "Start with a username and secure password." : "Sign in to continue editing."}</p>

        <form onSubmit={submit}>
          <label>
            Username
            <input
              autoComplete="username"
              value={form.username}
              onChange={(event) => setForm({ ...form, username: event.target.value })}
              required
            />
          </label>

          <label>
            Password
            <input
              autoComplete={isRegister ? "new-password" : "current-password"}
              type="password"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              required
            />
          </label>

          {isRegister && (
            <label>
              Confirm password
              <input
                autoComplete="new-password"
                type="password"
                value={form.confirmPassword}
                onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })}
                required
              />
            </label>
          )}

          {error && <p className="form-error">{error}</p>}

          <button className="primary-button" disabled={busy}>
            {busy ? "Working..." : isRegister ? "Create account" : "Sign in"}
          </button>
        </form>

        <button className="text-button" onClick={() => navigate(isRegister ? "/login" : "/register")}>
          {isRegister ? "Already have an account? Sign in" : "Need an account? Register"}
        </button>
      </section>
    </main>
  );
}

function Dashboard({ username, onLogout, showNotice }) {
  const [profile, setProfile] = useState(emptyProfile);
  const [skillsText, setSkillsText] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    getProfile()
      .then((data) => {
        if (!active) {
          return;
        }
        const nextProfile = hydrateProfile(data);
        setProfile(nextProfile);
        setSkillsText(nextProfile.skills.join("\n"));
      })
      .catch((err) => setError(err.message))
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const updateField = (field, value) => {
    setProfile((current) => ({ ...current, [field]: value }));
  };

  const updateCollection = (collection, index, field, value) => {
    setProfile((current) => ({
      ...current,
      [collection]: current[collection].map((item, itemIndex) =>
        itemIndex === index ? { ...item, [field]: value } : item
      )
    }));
  };

  const addItem = (collection, item) => {
    setProfile((current) => ({
      ...current,
      [collection]: [...current[collection], item]
    }));
  };

  const removeItem = (collection, index) => {
    setProfile((current) => ({
      ...current,
      [collection]: current[collection].filter((_, itemIndex) => itemIndex !== index)
    }));
  };

  const save = async () => {
    setSaving(true);
    setError("");
    try {
      await saveProfile(toSavePayload(profile, skillsText));
      showNotice("Resume saved.");
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const exportPdf = async () => {
    setError("");
    try {
      const response = await fetch("/api/profile/export/pdf", { credentials: "include" });
      if (!response.ok) {
        throw new Error(response.statusText || "Failed to export PDF");
      }
      const blob = await response.blob();
      const filename = filenameFrom(response) || `${username}_Resume.pdf`;
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = filename;
      anchor.click();
      URL.revokeObjectURL(url);
      showNotice("PDF export started.");
    } catch (err) {
      setError(err.message);
    }
  };

  const generateShare = async (settings = {}) => {
    try {
      const share = await generateShareLink(settings);
      setProfile((current) => ({ ...current, ...share }));
      showNotice("Share link is active.");
    } catch (err) {
      setError(err.message);
    }
  };

  const saveShareSettings = async (settings = {}) => {
    try {
      const share = await updateShareSettings(settings);
      setProfile((current) => ({ ...current, ...share }));
      showNotice("Share settings saved.");
    } catch (err) {
      setError(err.message);
    }
  };

  const revokeShare = async () => {
    try {
      const share = await revokeShareLink();
      setProfile((current) => ({ ...current, ...share }));
      showNotice("Share link revoked.");
    } catch (err) {
      setError(err.message);
    }
  };

  const copyShare = async () => {
    if (!profile.shareUrl) {
      return;
    }
    const absoluteUrl = new URL(profile.shareUrl, window.location.origin).toString();
    await navigator.clipboard.writeText(absoluteUrl);
    showNotice("Share link copied.");
  };

  if (loading) {
    return <Splash />;
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Signed in as {username}</p>
          <h1>Resume workspace</h1>
        </div>
        <div className="topbar-actions">
          <button className="ghost-button" onClick={exportPdf}>Export PDF</button>
          <button className="ghost-button" onClick={onLogout}>Logout</button>
        </div>
      </header>

      {error && <p className="inline-error">{error}</p>}

      <section className="workspace">
        <ProfileEditor
          profile={profile}
          skillsText={skillsText}
          saving={saving}
          updateField={updateField}
          setSkillsText={setSkillsText}
          updateCollection={updateCollection}
          addItem={addItem}
          removeItem={removeItem}
          save={save}
        />

        <aside className="preview-rail">
          <SharePanel
            profile={profile}
            generateShare={generateShare}
            saveShareSettings={saveShareSettings}
            revokeShare={revokeShare}
            copyShare={copyShare}
          />
          <ResumePreview profile={{ ...profile, skills: splitLines(skillsText) }} />
        </aside>
      </section>
    </main>
  );
}

function ProfileEditor({
  profile,
  skillsText,
  saving,
  updateField,
  setSkillsText,
  updateCollection,
  addItem,
  removeItem,
  save
}) {
  return (
    <section className="editor">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Editor</p>
          <h2>Profile details</h2>
        </div>
        <button className="primary-button compact" onClick={save} disabled={saving}>
          {saving ? "Saving..." : "Save changes"}
        </button>
      </div>

      <div className="field-grid">
        <Input label="First name" value={profile.firstName} onChange={(value) => updateField("firstName", value)} />
        <Input label="Last name" value={profile.lastName} onChange={(value) => updateField("lastName", value)} />
        <Input label="Email" type="email" value={profile.email} onChange={(value) => updateField("email", value)} />
        <Input label="Phone" value={profile.phone} onChange={(value) => updateField("phone", value)} />
        <Input label="Designation" value={profile.designation} onChange={(value) => updateField("designation", value)} />
        <label>
          Theme
          <select value={profile.theme || 1} onChange={(event) => updateField("theme", Number(event.target.value))}>
            <option value={1}>Classic amber</option>
            <option value={2}>One page</option>
            <option value={3}>Editorial</option>
          </select>
        </label>
      </div>

      <label>
        Summary
        <textarea
          rows={5}
          value={profile.summary || ""}
          onChange={(event) => updateField("summary", event.target.value)}
          placeholder="A concise professional summary works best."
        />
      </label>

      <label>
        Skills
        <textarea
          rows={4}
          value={skillsText}
          onChange={(event) => setSkillsText(event.target.value)}
          placeholder="One skill per line."
        />
      </label>

      <CollectionSection
        title="Work experience"
        addLabel="Add job"
        onAdd={() =>
          addItem("jobs", {
            company: "",
            designation: "",
            startDate: "",
            endDate: "",
            currentJob: false,
            responsibilities: []
          })
        }
      >
        {profile.jobs.map((job, index) => (
          <div className="collection-item" key={`job-${index}`}>
            <div className="item-heading">
              <strong>Job {index + 1}</strong>
              <button className="text-button danger" onClick={() => removeItem("jobs", index)}>Remove</button>
            </div>
            <div className="field-grid">
              <Input label="Company" value={job.company} onChange={(value) => updateCollection("jobs", index, "company", value)} />
              <Input label="Role" value={job.designation} onChange={(value) => updateCollection("jobs", index, "designation", value)} />
              <Input label="Start date" type="date" value={job.startDate} onChange={(value) => updateCollection("jobs", index, "startDate", value)} />
              <Input label="End date" type="date" value={job.endDate} onChange={(value) => updateCollection("jobs", index, "endDate", value)} />
            </div>
            <label className="checkbox-row">
              <input
                type="checkbox"
                checked={Boolean(job.currentJob)}
                onChange={(event) => updateCollection("jobs", index, "currentJob", event.target.checked)}
              />
              Current job
            </label>
            <label>
              Responsibilities
              <textarea
                rows={4}
                value={(job.responsibilities || []).join("\n")}
                onChange={(event) =>
                  updateCollection("jobs", index, "responsibilities", splitLines(event.target.value))
                }
                placeholder="One responsibility per line."
              />
            </label>
          </div>
        ))}
      </CollectionSection>

      <CollectionSection
        title="Education"
        addLabel="Add education"
        onAdd={() =>
          addItem("educations", {
            college: "",
            qualification: "",
            startDate: "",
            endDate: "",
            summary: ""
          })
        }
      >
        {profile.educations.map((education, index) => (
          <div className="collection-item" key={`education-${index}`}>
            <div className="item-heading">
              <strong>Education {index + 1}</strong>
              <button className="text-button danger" onClick={() => removeItem("educations", index)}>Remove</button>
            </div>
            <div className="field-grid">
              <Input label="School" value={education.college} onChange={(value) => updateCollection("educations", index, "college", value)} />
              <Input label="Qualification" value={education.qualification} onChange={(value) => updateCollection("educations", index, "qualification", value)} />
              <Input label="Start date" type="date" value={education.startDate} onChange={(value) => updateCollection("educations", index, "startDate", value)} />
              <Input label="End date" type="date" value={education.endDate} onChange={(value) => updateCollection("educations", index, "endDate", value)} />
            </div>
            <label>
              Summary
              <textarea
                rows={3}
                value={education.summary || ""}
                onChange={(event) => updateCollection("educations", index, "summary", event.target.value)}
              />
            </label>
          </div>
        ))}
      </CollectionSection>
    </section>
  );
}

function Input({ label, value, onChange, type = "text" }) {
  return (
    <label>
      {label}
      <input type={type} value={value || ""} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function CollectionSection({ title, addLabel, onAdd, children }) {
  return (
    <section className="collection-section">
      <div className="section-heading small">
        <h3>{title}</h3>
        <button className="ghost-button compact" onClick={onAdd}>{addLabel}</button>
      </div>
      <div className="collection-list">{children}</div>
    </section>
  );
}

function SharePanel({ profile, generateShare, saveShareSettings, revokeShare, copyShare }) {
  const shareUrl = profile.shareUrl ? new URL(profile.shareUrl, window.location.origin).toString() : "";
  const [expiresAt, setExpiresAt] = useState(toDateTimeLocal(profile.shareExpiresAt));
  const [maxViews, setMaxViews] = useState(profile.shareMaxViews || "");
  const [password, setPassword] = useState("");
  const [clearPassword, setClearPassword] = useState(false);

  useEffect(() => {
    setExpiresAt(toDateTimeLocal(profile.shareExpiresAt));
    setMaxViews(profile.shareMaxViews || "");
    setPassword("");
    setClearPassword(false);
  }, [profile.shareExpiresAt, profile.shareMaxViews, profile.shareToken]);

  const settingsPayload = () => ({
    expiresAt: expiresAt || null,
    maxViews: maxViews || null,
    password,
    clearPassword
  });

  const submitSettings = () => {
    if (profile.isPublic) {
      saveShareSettings(settingsPayload());
    } else {
      generateShare(settingsPayload());
    }
  };

  return (
    <section className="share-panel">
      <div>
        <p className="eyebrow">Public link</p>
        <h2>{profile.isPublic ? "Share is active" : "Share is private"}</h2>
        <p>{profile.isPublic ? shareUrl : "Generate a tokenized public resume link when you are ready."}</p>
        {profile.isPublic && (
          <div className="share-stats">
            <span>Views: {profile.shareViewCount || 0}{profile.shareMaxViews ? ` / ${profile.shareMaxViews}` : ""}</span>
            <span>Password: {profile.sharePasswordProtected ? "Enabled" : "Off"}</span>
            <span>Expires: {profile.shareExpiresAt ? formatDateTime(profile.shareExpiresAt) : "Never"}</span>
            {profile.shareLastViewedAt && <span>Last viewed: {formatDateTime(profile.shareLastViewedAt)}</span>}
          </div>
        )}
      </div>

      <div className="share-settings">
        <label>
          Expires at
          <input type="datetime-local" value={expiresAt} onChange={(event) => setExpiresAt(event.target.value)} />
        </label>
        <label>
          Max views
          <input
            type="number"
            min="1"
            value={maxViews}
            onChange={(event) => setMaxViews(event.target.value)}
            placeholder="Unlimited"
          />
        </label>
        <label>
          {profile.sharePasswordProtected ? "New password" : "Password"}
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder={profile.sharePasswordProtected ? "Leave blank to keep current" : "Optional"}
          />
        </label>
        {profile.sharePasswordProtected && (
          <label className="checkbox-row">
            <input
              type="checkbox"
              checked={clearPassword}
              onChange={(event) => setClearPassword(event.target.checked)}
            />
            Remove password
          </label>
        )}
      </div>

      <div className="button-row">
        {profile.isPublic ? (
          <>
            <button className="ghost-button compact" onClick={submitSettings}>Save settings</button>
            <button className="ghost-button compact" onClick={copyShare}>Copy</button>
            <button className="ghost-button compact danger" onClick={revokeShare}>Revoke</button>
          </>
        ) : (
          <button className="ghost-button compact" onClick={submitSettings}>Generate</button>
        )}
      </div>
    </section>
  );
}

function PublicSharePage() {
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState("");
  const [passwordRequired, setPasswordRequired] = useState(false);
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const token = new URLSearchParams(window.location.search).get("token");

  const loadPublicProfile = async (sharePassword) => {
    if (!token) {
      setError("Missing share token.");
      return;
    }

    setBusy(true);
    setError("");
    try {
      const data = await getPublicProfile(token, sharePassword);
      setProfile(hydrateProfile(data));
      setPasswordRequired(false);
    } catch (err) {
      if (err.status === 401 && err.data?.requiresPassword) {
        setPasswordRequired(true);
        setError(sharePassword ? err.message : "");
      } else {
        setError(err.message);
      }
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    if (!token) {
      setError("Missing share token.");
      return undefined;
    }

    let active = true;
    getPublicProfile(token)
      .then((data) => {
        if (active) {
          setProfile(hydrateProfile(data));
          setPasswordRequired(false);
        }
      })
      .catch((err) => {
        if (!active) {
          return;
        }
        if (err.status === 401 && err.data?.requiresPassword) {
          setPasswordRequired(true);
          setError("");
        } else {
          setError(err.message);
        }
      });

    return () => {
      active = false;
    };
  }, [token]);

  const submitPassword = (event) => {
    event.preventDefault();
    loadPublicProfile(password);
  };

  return (
    <main className="public-shell">
      <button className="text-button" onClick={() => navigate("/login")}>Back to login</button>
      {passwordRequired ? (
        <section className="auth-panel password-panel">
          <p className="eyebrow">Protected resume</p>
          <h2>Password required</h2>
          <p>This public resume is protected. Enter the share password to continue.</p>
          <form onSubmit={submitPassword}>
            <label>
              Share password
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoFocus
                required
              />
            </label>
            {error && <p className="form-error">{error}</p>}
            <button className="primary-button" disabled={busy}>
              {busy ? "Checking..." : "Unlock resume"}
            </button>
          </form>
        </section>
      ) : error ? (
        <p className="inline-error">{error}</p>
      ) : profile ? (
        <ResumePreview profile={profile} publicMode />
      ) : (
        <Splash />
      )}
    </main>
  );
}

function ResumePreview({ profile, publicMode = false }) {
  const name = [profile.firstName, profile.lastName].filter(Boolean).join(" ") || "Your Name";
  const jobs = profile.jobs || [];
  const educations = profile.educations || [];
  const skills = profile.skills || [];

  return (
    <article className={`resume-preview theme-${profile.theme || 1}`}>
      <header>
        <p className="eyebrow">{publicMode ? "Public resume" : "Live preview"}</p>
        <h2>{name}</h2>
        <p>{profile.designation || "Professional title"}</p>
        {!publicMode && (
          <p className="contact-line">
            {[profile.email, profile.phone].filter(Boolean).join(" / ") || "Contact details"}
          </p>
        )}
      </header>

      {profile.summary && (
        <section>
          <h3>Profile</h3>
          <p>{profile.summary}</p>
        </section>
      )}

      {skills.length > 0 && (
        <section>
          <h3>Skills</h3>
          <div className="skill-cloud">
            {skills.map((skill, index) => <span key={`${skill}-${index}`}>{skill}</span>)}
          </div>
        </section>
      )}

      {jobs.length > 0 && (
        <section>
          <h3>Experience</h3>
          {jobs.map((job, index) => (
            <div className="resume-entry" key={`preview-job-${index}`}>
              <div>
                <strong>{job.designation || "Role"}</strong>
                <span>{job.company || "Company"}</span>
              </div>
              <small>{dateRange(job)}</small>
              {(job.responsibilities || []).length > 0 && (
                <ul>
                  {job.responsibilities.map((item, itemIndex) => (
                    <li key={`preview-job-${index}-${itemIndex}`}>{item}</li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </section>
      )}

      {educations.length > 0 && (
        <section>
          <h3>Education</h3>
          {educations.map((education, index) => (
            <div className="resume-entry" key={`preview-education-${index}`}>
              <div>
                <strong>{education.college || "School"}</strong>
                <span>{education.qualification || "Qualification"}</span>
              </div>
              <small>{dateRange(education)}</small>
              {education.summary && <p>{education.summary}</p>}
            </div>
          ))}
        </section>
      )}
    </article>
  );
}

function hydrateProfile(profile) {
  return {
    ...emptyProfile,
    ...profile,
    jobs: profile.jobs || [],
    educations: profile.educations || [],
    skills: profile.skills || [],
    theme: profile.theme || 1
  };
}

function toSavePayload(profile, skillsText) {
  return {
    firstName: profile.firstName,
    lastName: profile.lastName,
    email: profile.email,
    phone: profile.phone,
    designation: profile.designation,
    summary: profile.summary,
    theme: Number(profile.theme) || 1,
    jobs: (profile.jobs || []).map((job) => ({
      company: job.company,
      designation: job.designation,
      startDate: job.startDate || null,
      endDate: job.currentJob ? null : job.endDate || null,
      currentJob: Boolean(job.currentJob),
      responsibilities: job.responsibilities || []
    })),
    educations: (profile.educations || []).map((education) => ({
      college: education.college,
      qualification: education.qualification,
      startDate: education.startDate || null,
      endDate: education.endDate || null,
      summary: education.summary
    })),
    skills: splitLines(skillsText)
  };
}

function splitLines(value) {
  return (value || "")
    .split(/\r?\n|,/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function dateRange(item) {
  const start = item.formattedStartDate || item.startDate || "";
  const end = item.currentJob ? "Present" : item.formattedEndDate || item.endDate || "";
  if (!start && !end) {
    return "";
  }
  return [start, end].filter(Boolean).join(" - ");
}

function toDateTimeLocal(value) {
  if (!value) {
    return "";
  }
  return value.slice(0, 16);
}

function formatDateTime(value) {
  if (!value) {
    return "";
  }
  const normalized = value.length === 16 ? `${value}:00` : value;
  const parsed = new Date(normalized);
  if (Number.isNaN(parsed.getTime())) {
    return value.replace("T", " ");
  }
  return parsed.toLocaleString();
}

function filenameFrom(response) {
  const disposition = response.headers.get("Content-Disposition") || "";
  const match = disposition.match(/filename="?([^"]+)"?/i);
  return match?.[1];
}
