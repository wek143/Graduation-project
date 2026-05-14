const ROUTES = {
    launch: "launch",
    login: "login",
    register: "register",
    admin: "admin",
    teacher: "teacher",
    student: "student"
};

const DEMO_ACCOUNTS = {
    admin: { username: "admin1", password: "123456" },
    teacher: { username: "teacher1", password: "123456" },
    student: { username: "student1", password: "123456" }
};

const DEFAULT_CODE = `public class Main {
    public static void main(String[] args) {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        int a = scanner.nextInt();
        int b = scanner.nextInt();
        System.out.println(a + b);
    }
}`;

const ADMIN_PAGE_SIZE = 6;
const ADMIN_LIST_CONFIG = {
    users: {
        label: "用户",
        endpoint: "/api/admin/users",
        getContainer: () => el.adminUserList,
        placeholder: "搜索账号、姓名或班级"
    },
    courses: {
        label: "课程",
        endpoint: "/api/admin/courses",
        getContainer: () => el.adminCourseList,
        placeholder: "搜索课程代码、名称、学期或教师"
    },
    assignments: {
        label: "作业",
        endpoint: "/api/admin/assignments",
        getContainer: () => el.adminAssignmentList,
        placeholder: "搜索作业标题、课程或教师"
    },
    auditLogs: {
        label: "日志",
        endpoint: "/api/admin/audit-logs",
        getContainer: () => el.adminAuditLogList,
        placeholder: "搜索动作、对象、操作人或摘要"
    }
};

const state = {
    auth: null,
    profile: null,
    assignments: [],
    courses: [],
    students: [],
    auditLogs: [],
    teacherStatistics: {
        courseStats: [],
        assignmentStats: [],
        filters: {
            keyword: "",
            courseId: "",
            status: ""
        }
    },
    selectedCourseEnrollments: [],
    selectedCourseId: null,
    editingCourseId: null,
    selectedAvailableStudentId: null,
    selectedEnrolledStudentId: null,
    latestSummaries: [],
    selectedSubmission: null,
    adminLists: createAdminListState(),
    activePortalModule: {
        teacher: null,
        student: null
    },
    route: ROUTES.launch,
    submissionPollTimer: null,
    submissionPollAttempts: 0
};

const el = {
    launchView: byId("launchView"),
    authView: byId("authView"),
    appView: byId("appView"),
    publicNav: byId("publicNav"),
    privateNav: byId("privateNav"),
    launchNavButton: byId("launchNavButton"),
    navLoginButton: byId("navLoginButton"),
    navRegisterButton: byId("navRegisterButton"),
    launchLoginButton: byId("launchLoginButton"),
    launchRegisterButton: byId("launchRegisterButton"),
    loginTabButton: byId("loginTabButton"),
    registerTabButton: byId("registerTabButton"),
    loginForm: byId("loginForm"),
    registerForm: byId("registerForm"),
    loginUsernameInput: byId("loginUsernameInput"),
    loginPasswordInput: byId("loginPasswordInput"),
    authTitle: byId("authTitle"),
    authSubtitle: byId("authSubtitle"),
    authMessage: byId("authMessage"),
    currentUsername: byId("currentUsername"),
    currentRoleLabel: byId("currentRoleLabel"),
    refreshDashboardButton: byId("refreshDashboardButton"),
    logoutButton: byId("logoutButton"),
    workspaceTitle: byId("workspaceTitle"),
    workspaceDescription: byId("workspaceDescription"),
    tokenExpiryLabel: byId("tokenExpiryLabel"),
    dashboardMessage: byId("dashboardMessage"),
    toastViewport: byId("toastViewport"),
    adminSection: byId("adminSection"),
    adminOverviewCards: byId("adminOverviewCards"),
    adminUserList: byId("adminUserList"),
    adminCourseList: byId("adminCourseList"),
    adminAssignmentList: byId("adminAssignmentList"),
    adminAuditLogList: byId("adminAuditLogList"),
    adminAiSettingsForm: byId("adminAiSettingsForm"),
    adminAiEnabledInput: byId("adminAiEnabledInput"),
    adminAiBaseUrlInput: byId("adminAiBaseUrlInput"),
    adminAiModelInput: byId("adminAiModelInput"),
    adminAiApiKeyInput: byId("adminAiApiKeyInput"),
    adminAiTimeoutInput: byId("adminAiTimeoutInput"),
    adminAiSettingsStatus: byId("adminAiSettingsStatus"),
    teacherSection: byId("teacherSection"),
    studentSection: byId("studentSection"),
    overviewCards: byId("overviewCards"),
    courseForm: byId("courseForm"),
    courseSubmitButton: byId("courseSubmitButton"),
    courseCancelEditButton: byId("courseCancelEditButton"),
    courseActiveToggle: byId("courseActiveToggle"),
    courseActiveInput: byId("courseActiveInput"),
    teacherCourses: byId("teacherCourses"),
    selectedCourseTitle: byId("selectedCourseTitle"),
    availableStudentList: byId("availableStudentList"),
    enrollSelectedButton: byId("enrollSelectedButton"),
    removeEnrollmentButton: byId("removeEnrollmentButton"),
    courseEnrollmentList: byId("courseEnrollmentList"),
    importUsersForm: byId("importUsersForm"),
    importCoursesForm: byId("importCoursesForm"),
    importEnrollmentsForm: byId("importEnrollmentsForm"),
    assignmentForm: byId("assignmentForm"),
    assignmentCourseSelect: byId("assignmentCourseSelect"),
    addTestCaseButton: byId("addTestCaseButton"),
    testCaseList: byId("testCaseList"),
    teacherAssignments: byId("teacherAssignments"),
    submissionAssignmentSelect: byId("submissionAssignmentSelect"),
    loadSubmissionsButton: byId("loadSubmissionsButton"),
    teacherSubmissions: byId("teacherSubmissions"),
    assignmentStatistics: byId("assignmentStatistics"),
    auditLogList: byId("auditLogList"),
    teacherSubmissionDetail: byId("teacherSubmissionDetail"),
    studentCourses: byId("studentCourses"),
    studentAssignments: byId("studentAssignments"),
    submissionAssignmentPicker: byId("submissionAssignmentPicker"),
    submissionForm: byId("submissionForm"),
    sourceCodeInput: byId("sourceCodeInput"),
    studentSummaries: byId("studentSummaries"),
    submissionDetail: byId("submissionDetail"),
    studentAiDiagnosisPanel: byId("studentAiDiagnosisPanel"),
    resetPasswordDialog: byId("resetPasswordDialog"),
    resetPasswordInput: byId("resetPasswordInput"),
    resetPasswordCancelButton: byId("resetPasswordCancelButton"),
    confirmDialog: byId("confirmDialog"),
    confirmDialogTitle: byId("confirmDialogTitle"),
    confirmDialogMessage: byId("confirmDialogMessage"),
    confirmDialogCancelButton: byId("confirmDialogCancelButton"),
    confirmDialogOkButton: byId("confirmDialogOkButton"),
    testCaseTemplate: byId("testCaseTemplate"),
    useDemoButtons: [...document.querySelectorAll(".use-demo-button")]
};

function createAdminListState() {
    return {
        users: { keyword: "", role: "", page: 0, size: ADMIN_PAGE_SIZE, data: null },
        courses: { keyword: "", page: 0, size: ADMIN_PAGE_SIZE, data: null },
        assignments: { keyword: "", page: 0, size: ADMIN_PAGE_SIZE, data: null },
        auditLogs: { keyword: "", page: 0, size: ADMIN_PAGE_SIZE, data: null }
    };
}

boot();

function boot() {
    initializePortalLayouts();
    bindEvents();
    startClockTicker();
    ensureDefaultTestCases();
    if (el.sourceCodeInput) {
        el.sourceCodeInput.value = DEFAULT_CODE;
    }
    setMessage(el.authMessage, "点击下方演示账号可快速填入。");
    setMessage(el.dashboardMessage, "登录后进入对应工作台。");
    handleRouteChange();
    const storedAuth = readJson("autograding-auth");
    if (isStoredAuthExpired(storedAuth)) {
        localStorage.removeItem("autograding-auth");
        return;
    }
    state.auth = storedAuth;
    if (state.auth?.token) {
        hydrateSession({ silent: true });
    }
}

function bindEvents() {
    window.addEventListener("hashchange", handleRouteChange);
    el.launchNavButton?.addEventListener("click", () => navigate(ROUTES.launch));
    el.navLoginButton?.addEventListener("click", () => navigate(ROUTES.login));
    el.navRegisterButton?.addEventListener("click", () => navigate(ROUTES.register));
    el.launchLoginButton?.addEventListener("click", () => navigate(ROUTES.login));
    el.launchRegisterButton?.addEventListener("click", () => navigate(ROUTES.register));
    el.loginTabButton?.addEventListener("click", () => navigate(ROUTES.login));
    el.registerTabButton?.addEventListener("click", () => navigate(ROUTES.register));
    el.loginForm?.addEventListener("submit", onLogin);
    el.registerForm?.addEventListener("submit", onRegister);
    el.logoutButton?.addEventListener("click", onLogout);
    el.refreshDashboardButton?.addEventListener("click", hydrateSession);
    el.adminAiSettingsForm?.addEventListener("submit", onSaveAiSettings);
    el.courseForm?.addEventListener("submit", onCreateCourse);
    el.courseCancelEditButton?.addEventListener("click", resetCourseForm);
    el.enrollSelectedButton?.addEventListener("click", onEnrollStudent);
    el.removeEnrollmentButton?.addEventListener("click", onRemoveEnrollment);
    el.importUsersForm?.addEventListener("submit", (event) => onImportCsv(event, "users"));
    el.importCoursesForm?.addEventListener("submit", (event) => onImportCsv(event, "courses"));
    el.importEnrollmentsForm?.addEventListener("submit", (event) => onImportCsv(event, "enrollments"));
    el.addTestCaseButton?.addEventListener("click", () => addTestCase());
    el.assignmentForm?.addEventListener("submit", onCreateAssignment);
    el.loadSubmissionsButton?.addEventListener("click", loadTeacherSubmissions);
    el.submissionForm?.addEventListener("submit", onSubmitCode);
    el.useDemoButtons.forEach((button) => {
        button.addEventListener("click", () => useDemoAccount(button.dataset.demoRole));
    });
}

function initializePortalLayouts() {
    initializeAdminPortal();
    initializeTeacherPortal();
    initializeStudentPortal();
}

function initializeAdminPortal() {
    const section = el.adminSection;
    if (!section || section.dataset.portalReady === "true") {
        return;
    }

    const overview = el.adminOverviewCards;
    const usersPanel = el.adminUserList?.closest(".portal-panel-card");
    const coursesPanel = el.adminCourseList?.closest(".portal-panel-card");
    const assignmentsPanel = el.adminAssignmentList?.closest(".portal-panel-card");
    const auditPanel = el.adminAuditLogList?.closest(".portal-panel-card");
    const shell = section.querySelector(".portal-shell");
    const main = shell?.querySelector(".portal-main");
    if (!shell || !main) {
        return;
    }

    if (overview) {
        const homePanel = section.querySelector('[data-module-panel="admin-home"]');
        homePanel?.appendChild(overview);
    }

    if (usersPanel) {
        wrapPortalCard(main, "admin-users", usersPanel);
    }
    if (coursesPanel) {
        wrapPortalCard(main, "admin-courses", coursesPanel);
    }
    if (assignmentsPanel) {
        wrapPortalCard(main, "admin-assignments", assignmentsPanel);
    }
    if (auditPanel) {
        wrapPortalCard(main, "admin-audit", auditPanel);
    }

    setupAdminListControls();
    bindPortalNav(section, "admin");
    section.dataset.portalReady = "true";
}

function initializeTeacherPortal() {
    const section = el.teacherSection;
    if (!section || section.dataset.portalReady === "true") {
        return;
    }

    const heading = section.querySelector(".section-heading");
    const overview = el.overviewCards;
    const coursesPanel = el.teacherCourses?.closest(".panel");
    const createPanel = el.assignmentForm?.closest(".panel");
    const libraryPanel = el.teacherAssignments?.closest(".panel");
    const submissionsPanel = el.teacherSubmissions?.closest(".panel");
    const submissionControls = el.submissionAssignmentSelect?.closest(".inline-form");
    const statsPanel = el.assignmentStatistics?.closest(".panel");
    const detailPanel = el.teacherSubmissionDetail?.closest(".panel");
    const importPanel = el.importUsersForm?.closest(".panel");
    const auditPanel = el.auditLogList?.closest(".panel");

    const shell = document.createElement("div");
    shell.className = "portal-shell";
    shell.innerHTML = `
        <aside class="portal-sidebar">
            <div class="portal-sidebar-title">教师功能</div>
            <button type="button" class="portal-nav-item" data-portal-role="teacher" data-module-target="">首页</button>
            <button type="button" class="portal-nav-item" data-portal-role="teacher" data-module-target="teacher-courses">课程与选课</button>
            <button type="button" class="portal-nav-item" data-portal-role="teacher" data-module-target="teacher-create">创建作业</button>
            <button type="button" class="portal-nav-item" data-portal-role="teacher" data-module-target="teacher-library">作业管理</button>
            <button type="button" class="portal-nav-item" data-portal-role="teacher" data-module-target="teacher-submissions">查看提交</button>
            <button type="button" class="portal-nav-item" data-portal-role="teacher" data-module-target="teacher-statistics">评分统计</button>
            <button type="button" class="portal-nav-item" data-portal-role="teacher" data-module-target="teacher-import">批量导入</button>
        </aside>
        <div class="portal-main"></div>
    `;
    const main = shell.querySelector(".portal-main");

    const homePanel = createPortalPanel("teacher-home");
    homePanel.innerHTML = `
        <div class="portal-home-card">
            <div class="portal-home-copy">
                <h3>XX大学欢迎您</h3>
                <p>请选择左侧功能进入相应业务操作。</p>
            </div>
            <div id="teacherClock" class="portal-home-clock">--:--:--</div>
        </div>
    `;
    if (overview) {
        homePanel.appendChild(overview);
    }
    main.appendChild(homePanel);

    movePanelToPortal(main, "teacher-courses", coursesPanel);
    movePanelToPortal(main, "teacher-create", createPanel);
    moveContentToPortal(main, "teacher-library", "作业与用例管理", el.teacherAssignments);
    moveContentToPortal(main, "teacher-submissions", "查看提交", submissionControls, el.teacherSubmissions, el.teacherSubmissionDetail);
    movePanelToPortal(main, "teacher-statistics", statsPanel);
    movePanelToPortal(main, "teacher-import", importPanel);

    heading?.insertAdjacentElement("afterend", shell);
    cleanupEmptyContainers(section);
    libraryPanel?.remove();
    submissionsPanel?.remove();
    detailPanel?.remove();
    auditPanel?.remove();
    bindPortalNav(section, "teacher");
    section.dataset.portalReady = "true";
}

function initializeStudentPortal() {
    const section = el.studentSection;
    if (!section || section.dataset.portalReady === "true") {
        return;
    }

    const heading = section.querySelector(".section-heading");
    const coursesPanel = el.studentCourses?.closest(".panel");
    const assignmentsPanel = el.studentAssignments?.closest(".panel");
    const submitPanel = el.submissionForm?.closest(".panel");
    const summariesPanel = el.studentSummaries?.closest(".panel");
    const detailPanel = el.submissionDetail?.closest(".panel");

    const shell = document.createElement("div");
    shell.className = "portal-shell";
    shell.innerHTML = `
        <aside class="portal-sidebar">
            <div class="portal-sidebar-title">学生功能</div>
            <button type="button" class="portal-nav-item" data-portal-role="student" data-module-target="">首页</button>
            <button type="button" class="portal-nav-item" data-portal-role="student" data-module-target="student-courses">课程选择</button>
            <button type="button" class="portal-nav-item" data-portal-role="student" data-module-target="student-assignments">已发布作业</button>
            <button type="button" class="portal-nav-item" data-portal-role="student" data-module-target="student-submit">代码提交</button>
            <button type="button" class="portal-nav-item" data-portal-role="student" data-module-target="student-results">最近提交</button>
            <button type="button" class="portal-nav-item" data-portal-role="student" data-module-target="student-detail">评测详情</button>
        </aside>
        <div class="portal-main"></div>
    `;
    const main = shell.querySelector(".portal-main");

    const homePanel = createPortalPanel("student-home");
    homePanel.innerHTML = `
        <div class="portal-home-card">
            <div class="portal-home-copy">
                <h3>XX大学欢迎您</h3>
                <p>请选择左侧功能进入相应业务操作。</p>
            </div>
            <div id="studentClock" class="portal-home-clock">--:--:--</div>
        </div>
    `;
    main.appendChild(homePanel);

    movePanelToPortal(main, "student-courses", coursesPanel);
    movePanelToPortal(main, "student-assignments", assignmentsPanel);
    movePanelToPortal(main, "student-submit", submitPanel);
    movePanelToPortal(main, "student-results", summariesPanel);
    movePanelToPortal(main, "student-detail", detailPanel);

    heading?.insertAdjacentElement("afterend", shell);
    cleanupEmptyContainers(section);
    bindPortalNav(section, "student");
    section.dataset.portalReady = "true";
}

function setupAdminListControls() {
    Object.entries(ADMIN_LIST_CONFIG).forEach(([key, config]) => {
        const container = config.getContainer();
        const card = container?.closest(".portal-panel-card");
        if (!container || !card || card.querySelector(`[data-admin-toolbar="${key}"]`)) {
            return;
        }

        const toolbar = document.createElement("div");
        toolbar.className = "admin-list-toolbar";
        toolbar.dataset.adminToolbar = key;
        toolbar.innerHTML = `
            <form class="admin-search-form" data-admin-search-form="${key}">
                <input type="search" name="keyword" placeholder="${config.placeholder}" autocomplete="off">
                ${key === "users" ? `
                    <select name="role">
                        <option value="">全部角色</option>
                        <option value="ADMIN">管理员</option>
                        <option value="TEACHER">教师</option>
                        <option value="STUDENT">学生</option>
                    </select>
                ` : ""}
                <button type="submit" class="btn btn-small btn-primary">搜索</button>
                <button type="button" class="btn btn-small btn-ghost" data-admin-clear="${key}">清空</button>
            </form>
            <div class="admin-list-pagination" data-admin-pagination="${key}"></div>
        `;
        card.insertBefore(toolbar, container);

        const form = toolbar.querySelector(`[data-admin-search-form="${key}"]`);
        const clearButton = toolbar.querySelector(`[data-admin-clear="${key}"]`);
        form?.addEventListener("submit", async (event) => {
            event.preventDefault();
            updateAdminListFilters(key, form);
            state.adminLists[key].page = 0;
            try {
                await loadAdminList(key);
            } catch (error) {
                notify(el.dashboardMessage, error.message, "error", `加载${config.label}失败`);
            }
        });
        clearButton?.addEventListener("click", async () => {
            resetAdminListFilters(key);
            syncAdminListControls(key);
            try {
                await loadAdminList(key);
            } catch (error) {
                notify(el.dashboardMessage, error.message, "error", `加载${config.label}失败`);
            }
        });
        syncAdminListControls(key);
    });
}

function createPortalPanel(moduleName) {
    const panel = document.createElement("section");
    panel.className = "portal-panel hidden";
    panel.dataset.modulePanel = moduleName;
    return panel;
}

function movePanelToPortal(main, moduleName, ...panels) {
    const modulePanel = createPortalPanel(moduleName);
    panels.filter(Boolean).forEach((panel) => {
        flattenPanelDetails(panel);
        modulePanel.appendChild(panel);
    });
    main.appendChild(modulePanel);
}

function moveContentToPortal(main, moduleName, title, ...nodes) {
    const modulePanel = createPortalPanel(moduleName);
    const card = document.createElement("div");
    card.className = "portal-panel-card";
    card.innerHTML = `<div class="portal-panel-head"><h3>${title}</h3></div>`;
    nodes.filter(Boolean).forEach((node) => card.appendChild(node));
    modulePanel.appendChild(card);
    main.appendChild(modulePanel);
}

function wrapPortalCard(main, moduleName, panelCard) {
    const modulePanel = main.querySelector(`[data-module-panel="${moduleName}"]`);
    if (modulePanel && panelCard && !modulePanel.contains(panelCard)) {
        modulePanel.appendChild(panelCard);
    }
}

function flattenPanelDetails(panel) {
    panel.querySelectorAll("details").forEach((details) => {
        details.open = true;
    });
    panel.classList.add("portal-flat-panel");
}

function bindPortalNav(section, role) {
    section.querySelectorAll(`.portal-nav-item[data-portal-role="${role}"]`).forEach((button) => {
        button.addEventListener("click", () => {
            state.activePortalModule[role] = button.dataset.moduleTarget || null;
            renderPortalModules();
        });
    });
}

function renderPortalModules() {
    renderPortalForRole("admin", state.route === ROUTES.admin);
    renderPortalForRole("teacher", state.route === ROUTES.teacher);
    renderPortalForRole("student", state.route === ROUTES.student);
}

function renderPortalForRole(role, active) {
    const section = role === "admin"
        ? el.adminSection
        : role === "teacher"
            ? el.teacherSection
            : el.studentSection;
    if (!section || !section.dataset.portalReady) {
        return;
    }
    const currentModule = state.activePortalModule[role];
    const visibleModule = currentModule || `${role}-home`;

    section.querySelectorAll(".portal-panel").forEach((panel) => {
        panel.classList.toggle("hidden", panel.dataset.modulePanel !== visibleModule);
    });

    section.querySelectorAll(`.portal-nav-item[data-portal-role="${role}"]`).forEach((button) => {
        const target = button.dataset.moduleTarget || `${role}-home`;
        button.classList.toggle("is-active", active && target === visibleModule);
    });
}

function cleanupEmptyContainers(section) {
    section.querySelectorAll(".workspace-grid").forEach((grid) => {
        if (!grid.querySelector(".panel")) {
            grid.remove();
        }
    });
}

function updateAdminListFilters(key, form) {
    const formData = new FormData(form);
    state.adminLists[key].keyword = String(formData.get("keyword") || "").trim();
    if (key === "users") {
        state.adminLists[key].role = String(formData.get("role") || "").trim();
    }
}

function resetAdminListFilters(key) {
    state.adminLists[key].keyword = "";
    state.adminLists[key].page = 0;
    if (key === "users") {
        state.adminLists[key].role = "";
    }
}

function syncAdminListControls(key) {
    const toolbar = document.querySelector(`[data-admin-toolbar="${key}"]`);
    if (!toolbar) {
        return;
    }
    const listState = state.adminLists[key];
    const keywordInput = toolbar.querySelector('input[name="keyword"]');
    if (keywordInput) {
        keywordInput.value = listState.keyword || "";
    }
    const roleSelect = toolbar.querySelector('select[name="role"]');
    if (roleSelect) {
        roleSelect.value = listState.role || "";
    }
}

function startClockTicker() {
    updatePortalClocks();
    window.setInterval(updatePortalClocks, 1000);
}

function updatePortalClocks() {
    const now = new Date();
    const timeText = new Intl.DateTimeFormat("zh-CN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit"
    }).format(now);
    const adminClock = byId("adminClock");
    const teacherClock = byId("teacherClock");
    const studentClock = byId("studentClock");
    if (adminClock) {
        adminClock.textContent = timeText;
    }
    if (teacherClock) {
        teacherClock.textContent = timeText;
    }
    if (studentClock) {
        studentClock.textContent = timeText;
    }
}

function isStoredAuthExpired(auth) {
    if (!auth?.expiresAt) {
        return false;
    }
    const expiresAt = new Date(auth.expiresAt);
    return !Number.isNaN(expiresAt.getTime()) && expiresAt.getTime() < Date.now();
}

function setSubmitButtonLoading(formElement, loadingText) {
    const button = formElement?.querySelector('[type="submit"]');
    if (!button) {
        return () => {};
    }
    const originalText = button.textContent;
    button.disabled = true;
    button.textContent = loadingText;
    return () => {
        button.disabled = false;
        button.textContent = originalText;
    };
}

function handleRouteChange() {
    const requestedRoute = getRouteFromHash();
    const route = resolveRoute(requestedRoute);
    state.route = route;
    if (route !== requestedRoute) {
        history.replaceState(null, "", `#/${route}`);
    }
    renderRoute(route);
}

function getRouteFromHash() {
    const raw = window.location.hash.replace(/^#\/?/, "");
    return Object.values(ROUTES).includes(raw) ? raw : ROUTES.launch;
}

function resolveRoute(route) {
    if (state.profile) {
        return routeForRole(state.profile.role);
    }
    if (isPrivateRoute(route)) {
        return ROUTES.login;
    }
    return route;
}

function renderRoute(route) {
    const isAuthRoute = route === ROUTES.login || route === ROUTES.register;
    const isAppRoute = route === ROUTES.admin || route === ROUTES.teacher || route === ROUTES.student;
    el.launchView?.classList.toggle("hidden", route !== ROUTES.launch);
    el.authView?.classList.toggle("hidden", !isAuthRoute);
    el.appView?.classList.toggle("hidden", !isAppRoute);
    el.publicNav?.classList.toggle("hidden", isAppRoute);
    el.privateNav?.classList.toggle("hidden", !isAppRoute);
    el.adminSection?.classList.toggle("hidden", route !== ROUTES.admin);
    el.teacherSection?.classList.toggle("hidden", route !== ROUTES.teacher);
    el.studentSection?.classList.toggle("hidden", route !== ROUTES.student);
    if (isAuthRoute) {
        switchAuthTab(route === ROUTES.login ? "login" : "register");
    }
    renderPortalModules();
}

async function onLogin(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const restoreSubmitButton = setSubmitButtonLoading(formElement, "登录中...");
    const form = new FormData(formElement);
    setMessage(el.authMessage, "正在登录，请稍候...");
    try {
        acceptAuth(await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({ username: form.get("username"), password: form.get("password") })
        }, false));
        await hydrateSession({ successMessage: "登录成功，已进入工作台。" });
    } catch (error) {
        notify(el.authMessage, error.message, "error", "登录失败");
    } finally {
        restoreSubmitButton();
    }
}

async function onRegister(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const restoreSubmitButton = setSubmitButtonLoading(formElement, "注册中...");
    const form = new FormData(formElement);
    setMessage(el.authMessage, "正在注册，请稍候...");
    try {
        acceptAuth(await api("/api/auth/register", {
            method: "POST",
            body: JSON.stringify({
                username: form.get("username"),
                password: form.get("password"),
                role: form.get("role"),
                fullName: form.get("fullName"),
                className: form.get("className")
            })
        }, false));
        await hydrateSession({ successMessage: "注册成功，已自动登录并进入工作台。" });
    } catch (error) {
        notify(el.authMessage, error.message, "error", "注册失败");
    } finally {
        restoreSubmitButton();
    }
}

async function onLogout() {
    try {
        if (state.auth?.token) {
            await api("/api/auth/logout", { method: "POST" });
        }
    } catch (error) {
        console.warn(error);
    }
    stopSubmissionPolling();
    state.auth = null;
    state.profile = null;
    state.assignments = [];
    state.courses = [];
    state.auditLogs = [];
    state.latestSummaries = [];
    state.selectedSubmission = null;
    state.adminLists = createAdminListState();
    writeAuth();
    resetWorkspace();
    renderSession();
    notify(el.authMessage, "已退出登录。", "success", "退出成功");
    setMessage(el.dashboardMessage, "已退出登录。", "success");
    navigate(ROUTES.launch);
}

async function hydrateSession(options = {}) {
    if (!state.auth?.token) {
        return;
    }
    if (!options.silent) {
        setMessage(el.dashboardMessage, "正在加载工作台数据...");
    }
    try {
        state.profile = await api("/api/auth/me");
        renderSession();
        navigate(routeForRole(state.profile.role), { replace: true });
        if (state.profile.role === "ADMIN") {
            await loadAdminDashboard(options.successMessage, options.silent === true);
        } else if (state.profile.role === "TEACHER") {
            await loadTeacherDashboard(options.successMessage, options.silent === true);
        } else {
            await loadStudentDashboard(options.successMessage, options.silent === true);
        }
    } catch (error) {
        state.auth = null;
        state.profile = null;
        writeAuth();
        resetWorkspace();
        renderSession();
        if (options.silent) {
            setMessage(el.authMessage, "点击下方演示账号可快速填入。");
            setMessage(el.dashboardMessage, "登录后进入对应工作台。");
        } else {
            notify(el.authMessage, error.message, "error", "登录状态异常");
            setMessage(el.dashboardMessage, error.message, "error");
        }
        navigate(ROUTES.login, { replace: true });
    }
}
async function loadTeacherDashboard(successMessage, suppressToast = false) {
    const [overview, assignments, assignmentStats, courseStats, courses, students] = await Promise.all([
        api("/api/users/overview"),
        api("/api/assignments"),
        api("/api/assignments/statistics/overview"),
        api("/api/courses/statistics/overview"),
        api("/api/courses"),
        api("/api/users/role/STUDENT")
    ]);

    state.assignments = assignments;
    state.courses = courses;
    state.students = students;
    state.teacherStatistics.courseStats = courseStats;
    state.teacherStatistics.assignmentStats = assignmentStats;

    el.overviewCards.innerHTML = [
        statCard("教师人数", overview.teacherCount, "账号总览"),
        statCard("学生人数", overview.studentCount, "参与用户"),
        statCard("作业总数", overview.assignmentCount, "全部作业"),
        statCard("已发布作业", overview.publishedAssignmentCount, "当前开放"),
        statCard("提交总量", overview.submissionCount, "评测记录")
    ].join("");

    renderTeacherCourses(courses);
    renderTeacherAssignments(assignments);
    renderTeacherStatistics();
    fillEntitySelect(el.assignmentCourseSelect, courses, "请选择课程", (course) => `${course.code} · ${course.name}`);
    fillEntitySelect(el.submissionAssignmentSelect, assignments, "选择作业查看提交", (assignment) => assignment.title);
    if (!courses.some((course) => String(course.id) === String(state.selectedCourseId))) {
        state.selectedCourseId = courses.length ? courses[0].id : null;
    }
    if (state.editingCourseId && !courses.some((course) => String(course.id) === String(state.editingCourseId))) {
        resetCourseForm();
    }
    await refreshSelectedCourseEnrollments();

    const message = successMessage || "教师端数据已刷新，可以继续发布作业、查看提交和统计信息。";
    if (suppressToast) {
        setMessage(el.dashboardMessage, message, "success");
    } else {
        notify(el.dashboardMessage, message, "success");
    }
}

async function loadAdminDashboard(successMessage, suppressToast = false) {
    const [overview, aiSettings] = await Promise.all([
        api("/api/users/overview"),
        api("/api/admin/ai-settings"),
        ...Object.keys(ADMIN_LIST_CONFIG).map((key) => loadAdminList(key))
    ]);

    el.adminOverviewCards.innerHTML = [
        statCard("教师人数", overview.teacherCount, "系统用户"),
        statCard("学生人数", overview.studentCount, "系统用户"),
        statCard("作业总数", overview.assignmentCount, "全局作业"),
        statCard("已发布作业", overview.publishedAssignmentCount, "当前开放"),
        statCard("提交总量", overview.submissionCount, "全局记录")
    ].join("");
    renderAdminAiSettings(aiSettings);

    const message = successMessage || "管理员工作台数据已刷新。";
    if (suppressToast) {
        setMessage(el.dashboardMessage, message, "success");
    } else {
        notify(el.dashboardMessage, message, "success");
    }
}

async function loadAdminList(key) {
    const config = ADMIN_LIST_CONFIG[key];
    const container = config?.getContainer();
    if (!config || !container) {
        return null;
    }

    renderAdminListLoading(key);
    syncAdminListControls(key);

    const pageData = await api(buildAdminListUrl(key));
    if (pageData.totalPages > 0 && pageData.page >= pageData.totalPages && state.adminLists[key].page > 0) {
        state.adminLists[key].page = pageData.totalPages - 1;
        return loadAdminList(key);
    }

    state.adminLists[key].page = pageData.page;
    state.adminLists[key].data = pageData;

    if (key === "users") {
        renderAdminUsers(pageData.content);
    } else if (key === "courses") {
        renderAdminCourses(pageData.content);
    } else if (key === "assignments") {
        renderAdminAssignments(pageData.content);
    } else if (key === "auditLogs") {
        renderAdminAuditLogs(pageData.content);
    }
    renderAdminPagination(key, pageData);
    return pageData;
}

function buildAdminListUrl(key) {
    const config = ADMIN_LIST_CONFIG[key];
    const listState = state.adminLists[key];
    const params = new URLSearchParams({
        page: String(Math.max(listState.page || 0, 0)),
        size: String(listState.size || ADMIN_PAGE_SIZE)
    });
    if (listState.keyword) {
        params.set("keyword", listState.keyword);
    }
    if (key === "users" && listState.role) {
        params.set("role", listState.role);
    }
    return `${config.endpoint}?${params.toString()}`;
}

function renderAdminListLoading(key) {
    const config = ADMIN_LIST_CONFIG[key];
    const container = config?.getContainer();
    if (!container) {
        return;
    }
    container.innerHTML = `<div class="empty-state">正在加载${config.label}列表...</div>`;
}

function renderAdminPagination(key, pageData) {
    const toolbar = document.querySelector(`[data-admin-toolbar="${key}"]`);
    const pagination = toolbar?.querySelector(`[data-admin-pagination="${key}"]`);
    if (!pagination || !pageData) {
        return;
    }

    const currentPage = pageData.totalElements === 0 ? 0 : pageData.page + 1;
    const totalPages = Math.max(pageData.totalPages, 1);
    pagination.innerHTML = `
        <span class="admin-page-summary">共 ${pageData.totalElements} 条</span>
        <div class="admin-page-actions">
            <button type="button" class="btn btn-small btn-secondary" data-admin-page-action="prev" ${pageData.first || pageData.totalElements === 0 ? "disabled" : ""}>上一页</button>
            <span class="admin-page-indicator">${currentPage} / ${totalPages}</span>
            <button type="button" class="btn btn-small btn-secondary" data-admin-page-action="next" ${pageData.last || pageData.totalElements === 0 ? "disabled" : ""}>下一页</button>
        </div>
    `;

    pagination.querySelectorAll("[data-admin-page-action]").forEach((button) => {
        button.addEventListener("click", async () => {
            const nextPage = state.adminLists[key].page + (button.dataset.adminPageAction === "prev" ? -1 : 1);
            state.adminLists[key].page = Math.max(nextPage, 0);
            try {
                await loadAdminList(key);
            } catch (error) {
                notify(el.dashboardMessage, error.message, "error", `加载${ADMIN_LIST_CONFIG[key].label}失败`);
            }
        });
    });
}

async function loadStudentDashboard(successMessage, suppressToast = false) {
    const [assignments, summaries, courses] = await Promise.all([
        api("/api/assignments/published"),
        api(`/api/submissions/student/${state.profile.id}/latest`),
        api("/api/courses")
    ]);

    state.assignments = assignments;
    state.courses = courses;
    state.latestSummaries = summaries;
    renderStudentCourses(courses, assignments);
    renderStudentAssignments(assignments);
    renderStudentSummaries(summaries);
    fillEntitySelect(el.submissionAssignmentPicker, assignments, "请选择要提交的作业", (assignment) => assignment.title);
    if (!el.submissionAssignmentPicker.value && assignments.length) {
        el.submissionAssignmentPicker.value = String(assignments[0].id);
    }

    const message = successMessage || "学生端数据已刷新，选择作业后即可提交代码。";
    if (suppressToast) {
        setMessage(el.dashboardMessage, message, "success");
    } else {
        notify(el.dashboardMessage, message, "success");
    }
}

function fillCourseForm(course) {
    if (!el.courseForm) {
        return;
    }
    el.courseForm.elements.code.value = course.code || "";
    el.courseForm.elements.name.value = course.name || "";
    el.courseForm.elements.term.value = course.term || "";
    el.courseForm.elements.className.value = course.className || "";
    if (el.courseActiveInput) {
        el.courseActiveInput.checked = course.active !== false;
    }
}

function resetCourseForm() {
    state.editingCourseId = null;
    el.courseForm?.reset();
    if (el.courseActiveInput) {
        el.courseActiveInput.checked = true;
    }
    el.courseActiveToggle?.classList.add("hidden");
    el.courseCancelEditButton?.classList.add("hidden");
    if (el.courseSubmitButton) {
        el.courseSubmitButton.textContent = "创建课程";
    }
}

function startCourseEdit(courseId) {
    const course = state.courses.find((item) => String(item.id) === String(courseId));
    if (!course) {
        notify(el.dashboardMessage, "未找到要编辑的课程。", "error", "编辑失败");
        return;
    }
    state.editingCourseId = Number(courseId);
    state.selectedCourseId = Number(courseId);
    fillCourseForm(course);
    el.courseActiveToggle?.classList.remove("hidden");
    el.courseCancelEditButton?.classList.remove("hidden");
    if (el.courseSubmitButton) {
        el.courseSubmitButton.textContent = "保存课程";
    }
    renderTeacherCourses(state.courses);
    refreshSelectedCourseEnrollments();
    el.courseForm?.scrollIntoView({ behavior: "smooth", block: "start" });
}

async function onDeleteCourse(courseId) {
    const course = state.courses.find((item) => String(item.id) === String(courseId));
    if (!course) {
        notify(el.dashboardMessage, "未找到要删除的课程。", "error", "删除失败");
        return;
    }
    const confirmed = await showConfirmDialog("删除课程", `确定删除课程“${course.code} · ${course.name}”吗？`);
    if (!confirmed) {
        return;
    }
    try {
        await api(`/api/courses/${courseId}`, { method: "DELETE" });
        if (String(state.selectedCourseId) === String(courseId)) {
            state.selectedCourseId = null;
        }
        if (String(state.editingCourseId) === String(courseId)) {
            resetCourseForm();
        }
        await loadTeacherDashboard("课程已删除。");
    } catch (error) {
        notify(el.dashboardMessage, error.message, "error", "删除课程失败");
    }
}

async function onCreateCourse(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const payload = {
        code: form.get("code"),
        name: form.get("name"),
        term: form.get("term"),
        className: form.get("className")
    };
    try {
        if (state.editingCourseId) {
            payload.active = form.get("active") === "on";
            const updatedCourse = await api(`/api/courses/${state.editingCourseId}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            state.selectedCourseId = updatedCourse.id;
            resetCourseForm();
            await loadTeacherDashboard("课程已更新。");
            return;
        }

        const createdCourse = await api("/api/courses", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        state.selectedCourseId = createdCourse.id;
        resetCourseForm();
        await loadTeacherDashboard("课程已创建。");
    } catch (error) {
        notify(el.dashboardMessage, error.message, "error",
            state.editingCourseId ? "更新课程失败" : "创建课程失败");
    }
}

async function onEnrollStudent() {
    const courseId = state.selectedCourseId;
    const studentId = state.selectedAvailableStudentId;

    if (!courseId || !studentId) {
        notify(el.dashboardMessage, "请先选择课程和待加入学生。", "error", "选课失败");
        return;
    }

    try {
        await api(`/api/courses/${courseId}/enrollments/${studentId}`, { method: "POST" });
        state.selectedAvailableStudentId = null;
        await refreshSelectedCourseEnrollments();
        notify(el.dashboardMessage, "学生已加入课程。", "success", "选课成功");
    } catch (error) {
        notify(el.dashboardMessage, error.message, "error", "选课失败");
    }
}

async function onRemoveEnrollment() {
    const courseId = state.selectedCourseId;
    const studentId = state.selectedEnrolledStudentId;

    if (!courseId || !studentId) {
        notify(el.dashboardMessage, "请先选择课程中的学生。", "error", "移出失败");
        return;
    }

    try {
        await api(`/api/courses/${courseId}/enrollments/${studentId}`, { method: "DELETE" });
        state.selectedEnrolledStudentId = null;
        await refreshSelectedCourseEnrollments();
        notify(el.dashboardMessage, "学生已移出课程。", "success", "移出成功");
    } catch (error) {
        notify(el.dashboardMessage, error.message, "error", "移出失败");
    }
}

async function onImportCsv(event, importType) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    try {
        const result = await api(`/api/import/${importType}`, {
            method: "POST",
            body: JSON.stringify({ csvContent: form.get("csvContent") })
        });
        formElement.reset();
        await loadTeacherDashboard(`导入完成：新增 ${result.createdCount}，跳过 ${result.skippedCount}。`);
    } catch (error) {
        notify(el.dashboardMessage, error.message, "error", "导入失败");
    }
}

async function onCreateAssignment(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const restoreSubmitButton = setSubmitButtonLoading(formElement, "发布中...");
    const form = new FormData(formElement);
    try {
        await api("/api/assignments", {
            method: "POST",
            body: JSON.stringify({
                title: form.get("title"),
                description: form.get("description"),
                deadline: normalizeDateTime(form.get("deadline")),
                courseId: toNullableNumber(form.get("courseId")),
                status: form.get("status"),
                maxSubmissions: Number(form.get("maxSubmissions") || 5),
                lateSubmissionAllowed: form.get("lateSubmissionAllowed") === "on",
                gradingPolicy: form.get("gradingPolicy"),
                testCases: collectTestCases()
            })
        });
        formElement.reset();
        el.testCaseList.innerHTML = "";
        ensureDefaultTestCases();
        await loadTeacherDashboard("作业已创建并刷新到列表中。");
    } catch (error) {
        notify(el.dashboardMessage, error.message, "error", "发布失败");
    } finally {
        restoreSubmitButton();
    }
}

async function loadTeacherSubmissions() {
    return loadTeacherSubmissionsWithOptions();
}

async function loadTeacherSubmissionsWithOptions(options = {}) {
    if (!el.submissionAssignmentSelect.value) {
        if (!options.silent) {
            notify(el.dashboardMessage, "请先选择一个作业。", "error", "操作失败");
        }
        return;
    }
    try {
        const submissions = await api(`/api/submissions/assignment/${el.submissionAssignmentSelect.value}`);
        renderTeacherSubmissions(submissions);
        if (!options.silent) {
            notify(el.dashboardMessage, `已加载 ${submissions.length} 条提交记录。`, "success", "加载成功");
        }
        return submissions;
    } catch (error) {
        if (!options.silent) {
            notify(el.dashboardMessage, error.message, "error", "加载失败");
            return null;
        }
        throw error;
    }
}

async function onSubmitCode(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const restoreSubmitButton = setSubmitButtonLoading(formElement, "提交中...");
    const form = new FormData(formElement);
    try {
        const submission = await api("/api/submissions", {
            method: "POST",
            body: JSON.stringify({
                assignmentId: Number(form.get("assignmentId")),
                sourceCode: form.get("sourceCode")
            })
        });
        state.selectedSubmission = submission;
        renderSubmissionDetail(el.submissionDetail, submission);
        if (submission.status === "PENDING") {
            startSubmissionPolling(submission.id, false);
        }
        await loadStudentDashboard("提交成功，已加入后台评测队列。");
    } catch (error) {
        notify(el.dashboardMessage, error.message, "error", "提交失败");
    } finally {
        restoreSubmitButton();
    }
}

function renderSession() {
    const authenticated = Boolean(state.profile && state.auth);
    const roleLabel = authenticated
        ? (state.profile.role === "ADMIN" ? "管理员" : state.profile.role === "TEACHER" ? "教师" : "学生")
        : "请先登录系统";

    el.currentUsername.textContent = authenticated ? state.profile.username : "未登录";
    el.currentRoleLabel.textContent = roleLabel;
    el.workspaceTitle.textContent = authenticated
        ? `XX大学编码作业自动批改平台 · ${roleLabel}工作台`
        : "XX大学编码作业自动批改平台";
    el.workspaceDescription.textContent = authenticated
        ? `当前角色：${roleLabel}`
        : "请先登录系统";
    el.tokenExpiryLabel.textContent = authenticated ? formatLoginStatus(state.auth.expiresAt) : "请先登录系统";
}
function renderTeacherCourses(items) {
    if (!el.teacherCourses) {
        return;
    }
    el.teacherCourses.innerHTML = items.length ? items.map((course) => `
        <article class="stack-item course-card ${String(state.selectedCourseId) === String(course.id) ? "is-active" : ""}" data-course-card-id="${course.id}">
            <div class="inline-header">
                <h4>${escapeHtml(course.code)} · ${escapeHtml(course.name)}</h4>
                <span class="pill ${course.active ? "status-published" : "status-closed"}">${course.active ? "进行中" : "已停用"}</span>
            </div>
            <div class="stack-meta">
                <span>学期：${escapeHtml(course.term)}</span>
                <span>班级：${escapeHtml(course.className || "-")}</span>
            </div>
            <div class="stack-actions">
                <button type="button" class="btn btn-small btn-secondary" data-course-edit-id="${course.id}">编辑</button>
                <button type="button" class="btn btn-small btn-ghost" data-course-delete-id="${course.id}">删除</button>
            </div>
        </article>
    `).join("") : `<div class="empty-state">当前还没有课程，先创建一个课程吧。</div>`;

    el.teacherCourses.querySelectorAll("[data-course-card-id]").forEach((card) => {
        card.addEventListener("click", async () => {
            state.selectedCourseId = Number(card.dataset.courseCardId);
            state.selectedAvailableStudentId = null;
            state.selectedEnrolledStudentId = null;
            renderTeacherCourses(state.courses);
            await refreshSelectedCourseEnrollments();
        });
    });

    el.teacherCourses.querySelectorAll("[data-course-edit-id]").forEach((button) => {
        button.addEventListener("click", (event) => {
            event.stopPropagation();
            startCourseEdit(button.dataset.courseEditId);
        });
    });

    el.teacherCourses.querySelectorAll("[data-course-delete-id]").forEach((button) => {
        button.addEventListener("click", async (event) => {
            event.stopPropagation();
            await onDeleteCourse(button.dataset.courseDeleteId);
        });
    });
}

function renderAvailableStudents(items) {
    if (!el.availableStudentList) {
        return;
    }
    if (!items.length) {
        el.availableStudentList.innerHTML = `
            <div class="empty-state">
                当前没有可加入的学生，或所有学生都已在该课程中。
            </div>
        `;
        state.selectedAvailableStudentId = null;
        return;
    }
    el.availableStudentList.innerHTML = items.map((item) => `
        <article class="stack-item transfer-item ${String(state.selectedAvailableStudentId) === String(item.id) ? "is-active" : ""}" data-available-student-id="${item.id}">
            <h4>${escapeHtml(item.fullName || item.username)}</h4>
            <div class="stack-meta">
                <span>账号：${escapeHtml(item.username)}</span>
                <span>班级：${escapeHtml(item.className || "-")}</span>
            </div>
        </article>
    `).join("");

    el.availableStudentList.querySelectorAll("[data-available-student-id]").forEach((card) => {
        card.addEventListener("click", () => {
            state.selectedAvailableStudentId = Number(card.dataset.availableStudentId);
            renderAvailableStudents(items);
        });
    });
}

function renderCourseEnrollments(items) {
    if (!el.courseEnrollmentList) {
        return;
    }
    el.courseEnrollmentList.innerHTML = items.length ? items.map((item) => `
        <article class="stack-item transfer-item ${String(state.selectedEnrolledStudentId) === String(item.studentId) ? "is-active" : ""}" data-enrolled-student-id="${item.studentId}">
            <div class="inline-header">
                <h4>${escapeHtml(item.studentFullName || item.studentUsername)}</h4>
                <span class="pill status-published">${formatDateTime(item.enrolledAt)}</span>
            </div>
            <div class="stack-meta">
                <span>账号：${escapeHtml(item.studentUsername)}</span>
                <span>班级：${escapeHtml(item.className || "-")}</span>
            </div>
        </article>
    `).join("") : `<div class="empty-state">当前课程还没有已选学生。</div>`;

    el.courseEnrollmentList.querySelectorAll("[data-enrolled-student-id]").forEach((card) => {
        card.addEventListener("click", () => {
            state.selectedEnrolledStudentId = Number(card.dataset.enrolledStudentId);
            renderCourseEnrollments(items);
        });
    });
}

function renderTeacherAssignments(assignments) {
    if (!assignments.length) {
        el.teacherAssignments.innerHTML = `<div class="empty-state">当前还没有作业，先创建一个新的作业吧。</div>`;
        return;
    }

    el.teacherAssignments.innerHTML = assignments.map((item) => `
        <article class="stack-item assignment-shell">
            <details class="assignment-accordion">
                <summary class="assignment-summary">
                    <div class="assignment-summary-main">
                        <div class="inline-header">
                            <h4>${escapeHtml(item.title)}</h4>
                            <span class="pill ${statusClass(item.status)}">${translateStatus(item.status)}</span>
                        </div>
                        <div class="stack-meta">
                            ${item.courseName ? `<span>课程：${escapeHtml(item.courseName)}</span>` : ""}
                            <span>截止时间：${formatDateTime(item.deadline)}</span>
                            <span>测试用例：${item.testCases.length} 条</span>
                            <span>最多提交：${item.maxSubmissions ?? 5} 次</span>
                        </div>
                    </div>
                    <span class="accordion-indicator" aria-hidden="true"></span>
                </summary>
                <div class="assignment-body">
                    <p>${escapeMultilineText(item.description)}</p>
                    <div class="stack-meta">
                        <span>${item.gradingPolicy === "HIGHEST" ? "按最高分计入" : "按最后一次计入"}</span>
                        <span>${item.lateSubmissionAllowed ? "允许逾期提交" : "不允许逾期提交"}</span>
                    </div>
                    <div class="stack-list compact-list">
                        ${item.testCases.map((testCase, index) => `
                            <div class="test-case-item">
                                <strong>用例 ${index + 1}</strong>
                                <div class="mini-meta"><span>输入：${escapeMultilineText(testCase.inputData)}</span></div>
                                <div class="mini-meta"><span>输出：${escapeMultilineText(testCase.expectedOutput)}</span></div>
                            </div>
                        `).join("")}
                    </div>
                    <div class="stack-actions">
                        <button class="btn btn-small btn-secondary" data-action="toggle-status" data-id="${item.id}">切换状态</button>
                        <button class="btn btn-small btn-ghost" data-action="delete-assignment" data-id="${item.id}">删除</button>
                    </div>
                </div>
            </details>
        </article>
    `).join("");

    el.teacherAssignments.querySelectorAll("[data-action='toggle-status']").forEach((button) => {
        button.addEventListener("click", async () => {
            const item = state.assignments.find((assignment) => String(assignment.id) === button.dataset.id);
            if (!item) {
                return;
            }
            const nextStatus = item.status === "PUBLISHED" ? "CLOSED" : item.status === "CLOSED" ? "DRAFT" : "PUBLISHED";
            try {
                await api(`/api/assignments/${item.id}`, {
                    method: "PUT",
                    body: JSON.stringify({
                        title: item.title,
                        description: item.description,
                        deadline: item.deadline,
                        status: nextStatus,
                        courseId: item.courseId,
                        maxSubmissions: item.maxSubmissions,
                        lateSubmissionAllowed: item.lateSubmissionAllowed,
                        gradingPolicy: item.gradingPolicy
                    })
                });
                await loadTeacherDashboard("作业状态已更新。");
            } catch (error) {
                notify(el.dashboardMessage, error.message, "error", "更新失败");
            }
        });
    });

    el.teacherAssignments.querySelectorAll("[data-action='delete-assignment']").forEach((button) => {
        button.addEventListener("click", async () => {
            const confirmed = await showConfirmDialog("删除作业", "删除后无法恢复，确认继续吗？");
            if (!confirmed) {
                return;
            }
            try {
                await api(`/api/assignments/${button.dataset.id}`, { method: "DELETE" });
                await loadTeacherDashboard("作业已删除。");
            } catch (error) {
                notify(el.dashboardMessage, error.message, "error", "删除失败");
            }
        });
    });
}

function renderAdminUsers(items) {
    if (!el.adminUserList) {
        return;
    }
    el.adminUserList.innerHTML = items.length ? items.map((item) => `
        <article class="stack-item">
            <div class="inline-header">
                <h4>${escapeHtml(item.fullName || item.username)}</h4>
                <span class="pill ${item.role === "ADMIN" ? "status-published" : item.role === "TEACHER" ? "status-draft" : "status-pending"}">${translateRole(item.role)}</span>
            </div>
            <div class="stack-meta">
                <span>账号：${escapeHtml(item.username)}</span>
                <span>班级：${escapeHtml(item.className || "-")}</span>
                <span>${item.active ? "状态：启用" : "状态：禁用"}</span>
            </div>
            <div class="stack-actions">
                <button class="btn btn-small btn-secondary" data-user-reset-id="${item.id}">重置密码</button>
                <button class="btn btn-small btn-ghost" data-user-toggle-id="${item.id}" data-user-active="${item.active}">
                    ${item.active ? "禁用账号" : "启用账号"}
                </button>
            </div>
        </article>
    `).join("") : `<div class="empty-state">暂无用户数据。</div>`;

    el.adminUserList.querySelectorAll("[data-user-reset-id]").forEach((button) => {
        button.addEventListener("click", async () => {
            const newPassword = await showResetPasswordDialog();
            if (newPassword === null) {
                return;
            }
            if (!newPassword.trim()) {
                notify(el.dashboardMessage, "新密码不能为空。", "error", "重置失败");
                return;
            }
            try {
                await api(`/api/users/${button.dataset.userResetId}/reset-password`, {
                    method: "POST",
                    body: JSON.stringify({ newPassword: newPassword.trim() })
                });
                await loadAdminDashboard("用户密码已重置。");
            } catch (error) {
                notify(el.dashboardMessage, error.message, "error", "重置密码失败");
            }
        });
    });

    el.adminUserList.querySelectorAll("[data-user-toggle-id]").forEach((button) => {
        button.addEventListener("click", async () => {
            const nextActive = button.dataset.userActive !== "true";
            const actionText = nextActive ? "启用" : "禁用";
            const confirmed = await showConfirmDialog(`${actionText}账号`, `确定要${actionText}该账号吗？`);
            if (!confirmed) {
                return;
            }
            try {
                await api(`/api/users/${button.dataset.userToggleId}/status`, {
                    method: "PUT",
                    body: JSON.stringify({ active: nextActive })
                });
                await loadAdminDashboard(`账号已${actionText}。`);
            } catch (error) {
                notify(el.dashboardMessage, error.message, "error", `${actionText}账号失败`);
            }
        });
    });
}

function renderAdminCourses(items) {
    if (!el.adminCourseList) {
        return;
    }
    el.adminCourseList.innerHTML = items.length ? items.map((item) => `
        <article class="stack-item">
            <div class="inline-header">
                <h4>${escapeHtml(item.code)} · ${escapeHtml(item.name)}</h4>
                <span class="pill ${item.active ? "status-published" : "status-closed"}">${item.active ? "进行中" : "已停用"}</span>
            </div>
            <div class="stack-meta">
                <span>教师：${escapeHtml(item.teacherName)}</span>
                <span>学期：${escapeHtml(item.term)}</span>
                <span>班级：${escapeHtml(item.className || "-")}</span>
            </div>
        </article>
    `).join("") : `<div class="empty-state">暂无课程数据。</div>`;
}

function renderAdminAssignments(items) {
    if (!el.adminAssignmentList) {
        return;
    }
    el.adminAssignmentList.innerHTML = items.length ? items.map((item) => `
        <article class="stack-item">
            <div class="inline-header">
                <h4>${escapeHtml(item.title)}</h4>
                <span class="pill ${statusClass(item.status)}">${translateStatus(item.status)}</span>
            </div>
            <div class="stack-meta">
                <span>教师：${escapeHtml(item.teacherName)}</span>
                <span>课程：${escapeHtml(item.courseName || "-")}</span>
                <span>截止时间：${formatDateTime(item.deadline)}</span>
            </div>
        </article>
    `).join("") : `<div class="empty-state">暂无作业数据。</div>`;
}

function renderAdminAuditLogs(items) {
    if (!el.adminAuditLogList) {
        return;
    }
    el.adminAuditLogList.innerHTML = items.length ? items.map((item) => `
        <article class="stack-item">
            <div class="inline-header">
                <h4>${escapeHtml(item.action)}</h4>
                <span class="pill status-published">${formatDateTime(item.createdAt)}</span>
            </div>
            <p>${escapeHtml(item.summary)}</p>
            <div class="stack-meta">
                <span>操作人：${escapeHtml(item.actorUsername || "-")}</span>
                <span>对象：${escapeHtml(item.targetType || "-")}</span>
                <span>ID：${escapeHtml(item.targetId || "-")}</span>
            </div>
        </article>
    `).join("") : `<div class="empty-state">暂无操作日志。</div>`;
}

function renderAdminAiSettings(settings) {
    if (!settings || !el.adminAiSettingsForm) {
        return;
    }
    el.adminAiEnabledInput.checked = Boolean(settings.enabled);
    el.adminAiBaseUrlInput.value = settings.baseUrl || "";
    el.adminAiModelInput.value = settings.model || "";
    el.adminAiApiKeyInput.value = "";
    el.adminAiTimeoutInput.value = String(settings.timeoutSeconds || 20);
    setMessage(
        el.adminAiSettingsStatus,
        settings.apiKeyConfigured
            ? "当前已配置 API Key，可直接使用 AI 辅助分析。"
            : "当前未配置 API Key，学生端无法调用 AI 分析。"
    );
}

async function onSaveAiSettings(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    try {
        const payload = {
            enabled: el.adminAiEnabledInput.checked,
            baseUrl: String(form.get("baseUrl") || "").trim(),
            model: String(form.get("model") || "").trim(),
            timeoutSeconds: Number(form.get("timeoutSeconds") || 20)
        };
        const apiKey = String(form.get("apiKey") || "").trim();
        if (apiKey) {
            payload.apiKey = apiKey;
        }
        const settings = await api("/api/admin/ai-settings", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        renderAdminAiSettings(settings);
        notify(el.dashboardMessage, "AI 配置已保存并立即生效。", "success", "保存成功");
    } catch (error) {
        notify(el.adminAiSettingsStatus, error.message, "error", "保存失败");
    }
}

function renderTeacherStatistics() {
    const courseStats = state.teacherStatistics.courseStats || [];
    const assignmentStats = state.teacherStatistics.assignmentStats || [];
    const filters = state.teacherStatistics.filters;
    const assignmentMetaById = new Map(state.assignments.map((assignment) => [String(assignment.id), assignment]));
    const courseOptions = state.courses.map((course) => `
        <option value="${course.id}" ${String(filters.courseId) === String(course.id) ? "selected" : ""}>
            ${escapeHtml(course.code)} · ${escapeHtml(course.name)}
        </option>
    `).join("");
    const normalizedKeyword = String(filters.keyword || "").trim().toLowerCase();

    const filteredCourseStats = courseStats.filter((item) => {
        if (filters.courseId && String(item.courseId) !== String(filters.courseId)) {
            return false;
        }
        if (!normalizedKeyword) {
            return true;
        }
        return [
            item.courseCode,
            item.courseName,
            item.term
        ].some((value) => String(value || "").toLowerCase().includes(normalizedKeyword));
    });

    const filteredAssignmentStats = assignmentStats.filter((item) => {
        const assignment = assignmentMetaById.get(String(item.assignmentId));
        if (filters.courseId && String(assignment?.courseId || "") !== String(filters.courseId)) {
            return false;
        }
        if (filters.status && String(item.assignmentStatus) !== String(filters.status)) {
            return false;
        }
        if (!normalizedKeyword) {
            return true;
        }
        return [
            item.assignmentTitle,
            assignment?.courseCode,
            assignment?.courseName
        ].some((value) => String(value || "").toLowerCase().includes(normalizedKeyword));
    });

    const summary = {
        courseCount: filteredCourseStats.length,
        assignmentCount: filteredAssignmentStats.length,
        totalSubmissions: filteredAssignmentStats.reduce((sum, item) => sum + (item.totalSubmissions || 0), 0),
        averageScore: filteredAssignmentStats.length
            ? filteredAssignmentStats.reduce((sum, item) => sum + (item.averageScore || 0), 0) / filteredAssignmentStats.length
            : 0
    };

    const hasCourseStats = courseStats.length > 0;
    const hasAssignmentStats = assignmentStats.length > 0;
    if (!hasCourseStats && !hasAssignmentStats) {
        el.assignmentStatistics.innerHTML = `<div class="empty-state">暂无统计数据。</div>`;
        return;
    }

    const toolbar = `
        <section class="statistics-toolbar-card">
            <div class="statistics-toolbar">
                <label class="statistics-filter grow">
                    <span>搜索统计</span>
                    <input id="statisticsKeywordInput" type="search" placeholder="搜索课程或作业" value="${escapeHtml(filters.keyword)}">
                </label>
                <label class="statistics-filter">
                    <span>课程筛选</span>
                    <select id="statisticsCourseFilter">
                        <option value="">全部课程</option>
                        ${courseOptions}
                    </select>
                </label>
                <label class="statistics-filter">
                    <span>状态筛选</span>
                    <select id="statisticsStatusFilter">
                        <option value="">全部状态</option>
                        <option value="DRAFT" ${filters.status === "DRAFT" ? "selected" : ""}>草稿</option>
                        <option value="PUBLISHED" ${filters.status === "PUBLISHED" ? "selected" : ""}>已发布</option>
                        <option value="CLOSED" ${filters.status === "CLOSED" ? "selected" : ""}>已关闭</option>
                    </select>
                </label>
                <div class="statistics-toolbar-actions">
                    <button id="statisticsClearButton" type="button" class="btn btn-small btn-ghost">清空筛选</button>
                </div>
            </div>
            <div class="statistics-summary-grid">
                ${statMiniCard("课程数", summary.courseCount)}
                ${statMiniCard("作业数", summary.assignmentCount)}
                ${statMiniCard("提交总量", summary.totalSubmissions)}
                ${statMiniCard("平均分", summary.averageScore.toFixed(1))}
            </div>
        </section>
    `;

    const courseSection = hasCourseStats ? `
        <section class="statistics-section">
            <div class="statistics-section-head">
                <h4>课程统计</h4>
                <span>按课程汇总选课、作业与提交情况，共 ${filteredCourseStats.length} 门</span>
            </div>
            <div class="stack-list">
                ${filteredCourseStats.length ? filteredCourseStats.map((item) => `
                    <article class="stack-item">
                        <div class="inline-header">
                            <h4>${escapeHtml(item.courseCode)} · ${escapeHtml(item.courseName)}</h4>
                            <span class="pill ${item.active ? "status-published" : "status-closed"}">${item.active ? "进行中" : "已停用"}</span>
                        </div>
                        <div class="stack-meta">
                            <span>学期：${escapeHtml(item.term)}</span>
                            <span>班级：${escapeHtml(item.className || "-")}</span>
                            <span>选课人数：${item.enrollmentCount}</span>
                            <span>作业总数：${item.assignmentCount}</span>
                            <span>已发布：${item.publishedAssignmentCount}</span>
                            <span>提交人数：${item.submittedStudentCount}</span>
                            <span>提交总量：${item.totalSubmissions}</span>
                            <span>平均分：${item.averageScore.toFixed(1)}</span>
                        </div>
                    </article>
                `).join("") : `<div class="empty-state">当前筛选下没有课程统计。</div>`}
            </div>
        </section>
    ` : "";

    const assignmentSection = hasAssignmentStats ? `
        <section class="statistics-section">
            <div class="statistics-section-head">
                <h4>作业统计</h4>
                <span>按作业查看评分表现，并支持导出成绩 CSV，共 ${filteredAssignmentStats.length} 个</span>
            </div>
            <div class="stack-list">
                ${filteredAssignmentStats.length ? filteredAssignmentStats.map((item) => {
                    const assignment = assignmentMetaById.get(String(item.assignmentId));
                    return `
                    <article class="stack-item">
                        <div class="inline-header">
                            <h4>${escapeHtml(item.assignmentTitle)}</h4>
                            <span class="pill ${statusClass(item.assignmentStatus)}">${translateStatus(item.assignmentStatus)}</span>
                        </div>
                        <div class="stack-meta">
                            <span>课程：${escapeHtml(assignment?.courseCode ? `${assignment.courseCode} · ${assignment.courseName}` : "-")}</span>
                            <span>提交次数：${item.totalSubmissions}</span>
                            <span>参与学生：${item.distinctStudentCount}</span>
                            <span>平均分：${item.averageScore.toFixed(1)}</span>
                        </div>
                        <div class="stack-actions">
                            <button class="btn btn-small btn-primary" data-stat-submission-assignment-id="${item.assignmentId}">查看提交</button>
                            <button class="btn btn-small btn-secondary" data-grade-export-id="${item.assignmentId}">导出成绩 CSV</button>
                        </div>
                    </article>
                `;
                }).join("") : `<div class="empty-state">当前筛选下没有作业统计。</div>`}
            </div>
        </section>
    ` : "";

    el.assignmentStatistics.innerHTML = `${toolbar}${courseSection}${assignmentSection}`;

    byId("statisticsKeywordInput")?.addEventListener("input", (event) => {
        state.teacherStatistics.filters.keyword = event.currentTarget.value;
        renderTeacherStatistics();
    });
    byId("statisticsCourseFilter")?.addEventListener("change", (event) => {
        state.teacherStatistics.filters.courseId = event.currentTarget.value;
        renderTeacherStatistics();
    });
    byId("statisticsStatusFilter")?.addEventListener("change", (event) => {
        state.teacherStatistics.filters.status = event.currentTarget.value;
        renderTeacherStatistics();
    });
    byId("statisticsClearButton")?.addEventListener("click", () => {
        state.teacherStatistics.filters.keyword = "";
        state.teacherStatistics.filters.courseId = "";
        state.teacherStatistics.filters.status = "";
        renderTeacherStatistics();
    });

    el.assignmentStatistics.querySelectorAll("[data-grade-export-id]").forEach((button) => {
        button.addEventListener("click", async () => {
            try {
                await downloadAssignmentGrades(button.dataset.gradeExportId);
                showToast("成绩导出已开始。", "success", "导出成功");
            } catch (error) {
                notify(el.dashboardMessage, error.message, "error", "导出失败");
            }
        });
    });

    el.assignmentStatistics.querySelectorAll("[data-stat-submission-assignment-id]").forEach((button) => {
        button.addEventListener("click", async () => {
            el.submissionAssignmentSelect.value = button.dataset.statSubmissionAssignmentId;
            state.activePortalModule.teacher = "teacher-submissions";
            renderPortalModules();
            try {
                await loadTeacherSubmissionsWithOptions({ silent: true });
                notify(el.dashboardMessage, "已切换到对应作业的提交列表。", "success", "定位成功");
            } catch (error) {
                notify(el.dashboardMessage, error.message, "error", "加载失败");
            }
        });
    });
}

function renderTeacherSubmissions(items) {
    el.teacherSubmissions.innerHTML = items.length ? items.map((item) => `
        <article class="stack-item">
            <div class="inline-header">
                <h4>${escapeHtml(item.studentName)} 的提交</h4>
                <span class="pill ${statusClass(item.status)}">${translateStatus(item.status)}</span>
            </div>
            <div class="stack-meta">
                <span>成绩：${item.score ?? 0}</span>
                <span>类名：${escapeHtml(item.className || "-")}</span>
                <span>提交时间：${formatDateTime(item.submittedAt)}</span>
            </div>
            <div class="stack-actions">
                <button class="btn btn-small btn-secondary" data-submission-id="${item.id}">查看详情</button>
                <button class="btn btn-small btn-ghost" data-submission-rejudge-id="${item.id}" ${item.status === "PENDING" ? "disabled" : ""}>
                    ${item.status === "PENDING" ? "评测中" : "重新判题"}
                </button>
            </div>
        </article>
    `).join("") : `<div class="empty-state">当前作业还没有学生提交。</div>`;

    el.teacherSubmissions.querySelectorAll("[data-submission-id]").forEach((button) => {
        button.addEventListener("click", async () => {
            try {
                const submission = await api(`/api/submissions/${button.dataset.submissionId}`);
                renderSubmissionDetail(el.teacherSubmissionDetail, submission);
                if (submission.status === "PENDING") {
                    startSubmissionPolling(submission.id, true);
                }
                showToast("已加载提交详情。", "info", "查看成功");
            } catch (error) {
                notify(el.dashboardMessage, error.message, "error", "加载失败");
            }
        });
    });

    el.teacherSubmissions.querySelectorAll("[data-submission-rejudge-id]").forEach((button) => {
        button.addEventListener("click", async () => {
            const confirmed = await showConfirmDialog("重新判题", "确定重新判题这条提交记录吗？");
            if (!confirmed) {
                return;
            }
            try {
                const submission = await api(`/api/submissions/${button.dataset.submissionRejudgeId}/rejudge`, {
                    method: "POST"
                });
                await loadTeacherSubmissionsWithOptions({ silent: true });
                renderSubmissionDetail(el.teacherSubmissionDetail, submission);
                startSubmissionPolling(submission.id, true);
                notify(el.dashboardMessage, "提交已重新加入评测队列。", "success", "重新判题成功");
            } catch (error) {
                notify(el.dashboardMessage, error.message, "error", "重新判题失败");
            }
        });
    });
}

function renderStudentAssignments(assignments) {
    el.studentAssignments.innerHTML = assignments.length ? assignments.map((item) => `
        <article class="assignment-card">
            <div class="inline-header">
                <h4>${escapeHtml(item.title)}</h4>
                <span class="pill ${statusClass(item.status)}">${translateStatus(item.status)}</span>
            </div>
            <p>${escapeMultilineText(item.description)}</p>
            <div class="card-meta">
                ${item.courseName ? `<span>课程：${escapeHtml(item.courseName)}</span>` : ""}
                <span>教师：${escapeHtml(item.teacherName)}</span>
                <span>截止时间：${formatDateTime(item.deadline)}</span>
                <span>测试用例：${item.testCases.length} 条</span>
            </div>
            <div class="stack-actions">
                <button class="btn btn-small btn-primary" data-assignment-select="${item.id}">选择并提交</button>
            </div>
        </article>
    `).join("") : `<div class="empty-state">当前暂无可提交的作业。</div>`;

    el.studentAssignments.querySelectorAll("[data-assignment-select]").forEach((button) => {
        button.addEventListener("click", () => {
            el.submissionAssignmentPicker.value = button.dataset.assignmentSelect;
            state.activePortalModule.student = "student-submit";
            renderPortalModules();
            focusSubmissionEditor();
            showToast("已切换到对应作业，可直接提交代码。", "info", "作业已选中");
        });
    });
}

function renderStudentCourses(courses, assignments) {
    if (!el.studentCourses) {
        return;
    }
    const assignmentCountByCourseId = assignments.reduce((map, assignment) => {
        if (assignment.courseId != null) {
            map.set(String(assignment.courseId), (map.get(String(assignment.courseId)) || 0) + 1);
        }
        return map;
    }, new Map());

    el.studentCourses.innerHTML = courses.length ? courses.map((course) => {
        const relatedAssignments = assignments.filter((assignment) => String(assignment.courseId || "") === String(course.id));
        const nearestDeadline = relatedAssignments
            .map((assignment) => assignment.deadline)
            .filter(Boolean)
            .sort((left, right) => new Date(left).getTime() - new Date(right).getTime())[0];
        return `
        <article class="assignment-card">
            <div class="inline-header">
                <h4>${escapeHtml(course.code)} · ${escapeHtml(course.name)}</h4>
                <span class="pill ${course.active ? "status-published" : "status-closed"}">${course.active ? "已选课程" : "已停用"}</span>
            </div>
            <p>教师：${escapeHtml(course.teacherName)}${course.className ? ` · 班级：${escapeHtml(course.className)}` : ""}</p>
            <div class="card-meta">
                <span>学期：${escapeHtml(course.term || "-")}</span>
                <span>已发布作业：${assignmentCountByCourseId.get(String(course.id)) || 0}</span>
                <span>下次截止：${nearestDeadline ? formatDateTime(nearestDeadline) : "暂无"}</span>
            </div>
            <div class="stack-actions">
                <button class="btn btn-small btn-primary" data-student-course-assignments="${course.id}">查看课程作业</button>
                <button class="btn btn-small btn-secondary" data-student-course-submit="${course.id}" ${relatedAssignments.length ? "" : "disabled"}>前往提交</button>
            </div>
        </article>
    `;
    }).join("") : `<div class="empty-state">当前还没有已选课程，请联系教师为你加入课程。</div>`;

    el.studentCourses.querySelectorAll("[data-student-course-assignments]").forEach((button) => {
        button.addEventListener("click", () => {
            state.activePortalModule.student = "student-assignments";
            renderPortalModules();
            showToast("已切换到课程作业列表。", "info", "查看成功");
        });
    });

    el.studentCourses.querySelectorAll("[data-student-course-submit]").forEach((button) => {
        button.addEventListener("click", () => {
            const courseAssignments = assignments.filter((assignment) => String(assignment.courseId || "") === button.dataset.studentCourseSubmit);
            if (!courseAssignments.length) {
                notify(el.dashboardMessage, "该课程当前还没有可提交的作业。", "error", "无法提交");
                return;
            }
            el.submissionAssignmentPicker.value = String(courseAssignments[0].id);
            state.activePortalModule.student = "student-submit";
            renderPortalModules();
            focusSubmissionEditor();
            showToast("已为你选中该课程下的作业。", "info", "准备提交");
        });
    });
}

function renderStudentSummaries(items) {
    el.studentSummaries.innerHTML = items.length ? items.map((item) => `
        <article class="stack-item">
            <div class="inline-header">
                <h4>${escapeHtml(item.assignmentTitle)}</h4>
                <span class="pill ${statusClass(item.status)}">${translateStatus(item.status)}</span>
            </div>
            <div class="stack-meta">
                <span>得分：${item.score ?? 0}</span>
                <span>提交时间：${formatDateTime(item.submittedAt)}</span>
            </div>
            <div class="stack-actions">
                <button class="btn btn-small btn-secondary" data-summary-id="${item.submissionId}">查看评测细节</button>
            </div>
        </article>
    `).join("") : `<div class="empty-state">你还没有任何提交记录。</div>`;

    el.studentSummaries.querySelectorAll("[data-summary-id]").forEach((button) => {
        button.addEventListener("click", async () => {
            try {
                const submission = await api(`/api/submissions/${button.dataset.summaryId}`);
                renderSubmissionDetail(el.submissionDetail, submission);
                if (submission.status === "PENDING") {
                    startSubmissionPolling(submission.id, false);
                }
                showToast("已加载评测详情。", "info", "查看成功");
            } catch (error) {
                notify(el.dashboardMessage, error.message, "error", "加载失败");
            }
        });
    });
}

function renderSubmissionDetail(target, submission) {
    state.selectedSubmission = submission;
    if (!submission) {
        target.className = "detail-panel empty-state";
        target.textContent = "暂无可展示的评测详情。";
        return;
    }
    const aiPanel = target === el.submissionDetail ? el.studentAiDiagnosisPanel : null;
    resetAiDiagnosisPanel(aiPanel);
    const caseResults = submission.caseResults || [];
    const cases = caseResults.length ? caseResults.map((item) => `
        <article class="case-result-card">
            <div class="inline-header">
                <strong>测试用例 ${item.caseOrder}</strong>
                <span class="pill ${item.passed ? "status-accepted" : "status-failed"}">${item.passed ? "通过" : "未通过"}</span>
            </div>
            <div class="detail-block"><strong>输入</strong><pre>${escapeHtml(item.inputData)}</pre></div>
            <div class="detail-block"><strong>期望输出</strong><pre>${escapeHtml(item.expectedOutput)}</pre></div>
            <div class="detail-block"><strong>实际输出</strong><pre>${escapeHtml(item.actualOutput || "")}</pre></div>
            ${item.errorMessage ? `<div class="detail-block"><strong>错误信息</strong><pre>${escapeHtml(item.errorMessage)}</pre></div>` : ""}
        </article>
    `).join("") : `<div class="empty-state">${submission.status === "PENDING" ? "评测正在后台执行，请稍候自动刷新。" : "该提交当前没有测试用例评测明细。"}</div>`;

    target.className = "detail-panel";
    target.innerHTML = `
        <article class="detail-block">
            <div class="inline-header">
                <h4>${escapeHtml(submission.assignmentTitle)}</h4>
                <span class="pill ${statusClass(submission.status)}">${translateStatus(submission.status)}</span>
            </div>
            <div class="stack-meta">
                <span>学生：${escapeHtml(submission.studentName)}</span>
                <span>类名：${escapeHtml(submission.className || "-")}</span>
                <span>分数：${submission.score ?? 0}</span>
                <span>提交时间：${formatDateTime(submission.submittedAt)}</span>
            </div>
            ${aiPanel ? `<div class="stack-actions">
                <button class="btn btn-small btn-secondary" data-ai-diagnosis-id="${submission.id}" ${submission.status === "PENDING" ? "disabled" : ""}>
                    ${submission.status === "PENDING" ? "评测中" : "AI 辅助分析"}
                </button>
            </div>` : ""}
        </article>
        <article class="detail-block"><strong>编译信息</strong><pre>${escapeHtml(submission.compileMessage || "")}</pre></article>
        <article class="detail-block"><strong>运行信息</strong><pre>${escapeHtml(submission.runtimeMessage || "")}</pre></article>
        <article class="detail-block"><strong>提交源码</strong><pre>${escapeHtml(submission.sourceCode || "")}</pre></article>
        ${cases}
    `;

    const aiButton = target.querySelector("[data-ai-diagnosis-id]");
    if (aiButton) {
        aiButton.addEventListener("click", async () => {
            await loadAiDiagnosis(submission.id, aiPanel, aiButton);
        });
    }
}

function resetAiDiagnosisPanel(target) {
    if (!target) {
        return;
    }
    target.className = "detail-panel empty-state";
    target.textContent = "发起 AI 辅助分析后，这里会显示问题概述、修改建议和知识点提示。";
}

async function loadAiDiagnosis(submissionId, target, button) {
    if (!target) {
        return;
    }
    const originalText = button?.textContent;
    if (button) {
        button.disabled = true;
        button.textContent = "分析中...";
    }
    target.className = "detail-panel";
    target.innerHTML = `<div class="detail-block"><strong>AI 辅助分析</strong><p class="detail-copy">正在生成分析结果，请稍候。</p></div>`;
    try {
        const diagnosis = await api(`/api/submissions/${submissionId}/ai-diagnosis`, { method: "POST" });
        renderAiDiagnosis(target, diagnosis);
        showToast("AI 辅助分析已生成。", "success", "分析完成");
    } catch (error) {
        target.className = "detail-panel empty-state";
        target.textContent = error.message || "AI 分析暂时不可用，请稍后重试。";
        showToast(error.message || "AI 分析暂时不可用，请稍后重试。", "error", "分析失败");
    } finally {
        if (button) {
            button.disabled = false;
            button.textContent = originalText || "AI 辅助分析";
        }
    }
}

function renderAiDiagnosis(target, diagnosis) {
    const possibleCauses = renderAiList(diagnosis.possibleCauses, "暂无明确原因分析。");
    const fixSuggestions = renderAiList(diagnosis.fixSuggestions, "暂无修改建议。");
    const knowledgePoints = renderAiList(diagnosis.knowledgePoints, "暂无相关知识点提示。");

    target.className = "detail-panel";
    target.innerHTML = `
        <article class="detail-block">
            <div class="inline-header">
                <h4>AI 辅助分析</h4>
                <span class="pill ${statusClass(diagnosis.status)}">${translateStatus(diagnosis.status)}</span>
            </div>
            <p class="detail-copy">${escapeHtml(diagnosis.summary || "暂无分析摘要。")}</p>
        </article>
        <article class="detail-block">
            <strong>可能原因</strong>
            ${possibleCauses}
        </article>
        <article class="detail-block">
            <strong>修改建议</strong>
            ${fixSuggestions}
        </article>
        <article class="detail-block">
            <strong>相关知识点</strong>
            ${knowledgePoints}
        </article>
        <article class="detail-block">
            <strong>说明</strong>
            <p class="detail-copy">${escapeHtml(diagnosis.disclaimer || "AI 分析仅供参考。")}</p>
        </article>
    `;
}

function renderAiList(items, emptyText) {
    if (!items || !items.length) {
        return `<p class="detail-copy">${escapeHtml(emptyText)}</p>`;
    }
    return `
        <ul class="detail-list">
            ${items.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}
        </ul>
    `;
}

function renderAuditLogs(items) {
    if (!el.auditLogList) {
        return;
    }
    el.auditLogList.innerHTML = items.length ? items.map((item) => `
        <article class="stack-item">
            <div class="inline-header">
                <h4>${escapeHtml(item.action)}</h4>
                <span class="pill status-published">${formatDateTime(item.createdAt)}</span>
            </div>
            <p>${escapeHtml(item.summary)}</p>
            <div class="stack-meta">
                <span>操作人：${escapeHtml(item.actorUsername || "-")}</span>
                <span>对象：${escapeHtml(item.targetType || "-")}</span>
                <span>ID：${escapeHtml(item.targetId || "-")}</span>
            </div>
        </article>
    `).join("") : `<div class="empty-state">暂无审计日志。</div>`;
}
function ensureDefaultTestCases() {
    if (el.testCaseList.children.length) {
        return;
    }
    addTestCase("1 2", "3");
    addTestCase("6 9", "15");
}

function addTestCase(input = "", output = "") {
    const fragment = el.testCaseTemplate.content.cloneNode(true);
    const item = fragment.querySelector(".test-case-item");
    item.querySelector(".case-input").value = input;
    item.querySelector(".case-output").value = output;
    item.querySelector(".remove-test-case").addEventListener("click", () => {
        item.remove();
        refreshTestCaseTitles();
    });
    el.testCaseList.appendChild(fragment);
    refreshTestCaseTitles();
    if (!input && !output) {
        showToast("已新增一个测试用例。", "info", "操作完成");
    }
}

function refreshTestCaseTitles() {
    [...el.testCaseList.querySelectorAll(".test-case-item")].forEach((item, index) => {
        item.querySelector(".case-title").textContent = `测试用例 ${index + 1}`;
    });
}

function collectTestCases() {
    return [...el.testCaseList.querySelectorAll(".test-case-item")]
        .map((item) => ({
            inputData: item.querySelector(".case-input").value.trim(),
            expectedOutput: item.querySelector(".case-output").value.trim()
        }))
        .filter((item) => item.inputData || item.expectedOutput);
}

function switchAuthTab(tab) {
    const isLogin = tab === "login";
    el.loginTabButton.classList.toggle("active", isLogin);
    el.registerTabButton.classList.toggle("active", !isLogin);
    el.loginForm.classList.toggle("hidden", !isLogin);
    el.registerForm.classList.toggle("hidden", isLogin);
    el.authTitle.textContent = isLogin ? "欢迎回来" : "创建账号";
    el.authSubtitle.textContent = isLogin ? "登录后进入对应工作台" : "注册完成后自动登录";
    setMessage(el.authMessage, isLogin ? "点击下方演示账号可快速填入。" : "注册后会自动登录。");
}

function useDemoAccount(role) {
    const demo = DEMO_ACCOUNTS[role];
    const roleLabel = role === "admin" ? "管理员" : role === "teacher" ? "教师" : "学生";
    // 确保当前在登录 tab
    if (el.loginForm?.classList.contains("hidden")) {
        switchAuthTab("login");
    }
    el.loginUsernameInput.value = demo.username;
    el.loginPasswordInput.value = demo.password;
    notify(el.authMessage, `已填入${roleLabel}演示账号，点击登录即可进入。`, "success", "快捷填充");
}

function showResetPasswordDialog() {
    return new Promise((resolve) => {
        if (!el.resetPasswordDialog || !el.resetPasswordInput) {
            resolve(null);
            return;
        }
        const form = el.resetPasswordDialog.querySelector("form");
        let settled = false;
        const settle = (value) => {
            if (settled) {
                return;
            }
            settled = true;
            form?.removeEventListener("submit", onSubmit);
            el.resetPasswordCancelButton?.removeEventListener("click", onCancel);
            el.resetPasswordDialog.removeEventListener("close", onClose);
            if (el.resetPasswordDialog.open) {
                el.resetPasswordDialog.close();
            }
            resolve(value);
        };
        const onSubmit = (event) => {
            event.preventDefault();
            settle(el.resetPasswordInput.value);
        };
        const onCancel = () => settle(null);
        const onClose = () => settle(null);

        el.resetPasswordInput.value = "";
        form?.addEventListener("submit", onSubmit);
        el.resetPasswordCancelButton?.addEventListener("click", onCancel);
        el.resetPasswordDialog.addEventListener("close", onClose);
        el.resetPasswordDialog.showModal();
        el.resetPasswordInput.focus();
    });
}

function showConfirmDialog(title, message) {
    return new Promise((resolve) => {
        if (!el.confirmDialog) {
            resolve(false);
            return;
        }
        let settled = false;
        const settle = (value) => {
            if (settled) {
                return;
            }
            settled = true;
            el.confirmDialogOkButton?.removeEventListener("click", onOk);
            el.confirmDialogCancelButton?.removeEventListener("click", onCancel);
            el.confirmDialog.removeEventListener("close", onClose);
            if (el.confirmDialog.open) {
                el.confirmDialog.close();
            }
            resolve(value);
        };
        const onOk = () => settle(true);
        const onCancel = () => settle(false);
        const onClose = () => settle(false);

        if (el.confirmDialogTitle) {
            el.confirmDialogTitle.textContent = title || "确认操作";
        }
        if (el.confirmDialogMessage) {
            el.confirmDialogMessage.textContent = message || "";
        }
        el.confirmDialogOkButton?.addEventListener("click", onOk);
        el.confirmDialogCancelButton?.addEventListener("click", onCancel);
        el.confirmDialog.addEventListener("close", onClose);
        el.confirmDialog.showModal();
        el.confirmDialogOkButton?.focus();
    });
}

function acceptAuth(auth) {
    state.auth = auth;
    state.profile = { id: auth.id, username: auth.username, role: auth.role };
    writeAuth();
}

function fillEntitySelect(select, items, placeholder, labelBuilder) {
    if (!select) {
        return;
    }
    const previousValue = select.value;
    select.innerHTML = [`<option value="">${placeholder}</option>`]
        .concat(items.map((item) => `<option value="${item.id}">${escapeHtml(labelBuilder(item))}</option>`))
        .join("");
    if (previousValue && items.some((item) => String(item.id) === String(previousValue))) {
        select.value = previousValue;
    }
}

function resetWorkspace() {
    stopSubmissionPolling();
    state.teacherStatistics = {
        courseStats: [],
        assignmentStats: [],
        filters: {
            keyword: "",
            courseId: "",
            status: ""
        }
    };
    state.selectedCourseId = null;
    state.editingCourseId = null;
    state.selectedAvailableStudentId = null;
    state.selectedEnrolledStudentId = null;
    state.selectedCourseEnrollments = [];
    resetCourseForm();
    if (el.overviewCards) {
        el.overviewCards.innerHTML = "";
    }
    if (el.teacherCourses) {
        el.teacherCourses.innerHTML = `<div class="empty-state">登录教师账号后显示课程列表。</div>`;
    }
    if (el.courseEnrollmentList) {
        el.courseEnrollmentList.innerHTML = `<div class="empty-state">选择课程后显示已选学生。</div>`;
    }
    if (el.availableStudentList) {
        el.availableStudentList.innerHTML = `<div class="empty-state">选择课程后显示可加入学生。</div>`;
    }
    if (el.selectedCourseTitle) {
        el.selectedCourseTitle.textContent = "选择课程后管理选课";
    }
    if (el.teacherAssignments) {
        el.teacherAssignments.innerHTML = `<div class="empty-state">登录教师账号后显示作业列表。</div>`;
    }
    if (el.teacherSubmissions) {
        el.teacherSubmissions.innerHTML = `<div class="empty-state">加载提交记录后会显示在这里。</div>`;
    }
    if (el.assignmentStatistics) {
        el.assignmentStatistics.innerHTML = `<div class="empty-state">统计信息会显示在这里。</div>`;
    }
    if (el.auditLogList) {
        el.auditLogList.innerHTML = `<div class="empty-state">操作日志会显示在这里。</div>`;
    }
    if (el.teacherSubmissionDetail) {
        el.teacherSubmissionDetail.className = "detail-panel empty-state";
        el.teacherSubmissionDetail.textContent = "选择一条教师端提交记录后，这里会显示编译结果、运行信息和测试用例详情。";
    }
    if (el.studentAssignments) {
        el.studentAssignments.innerHTML = `<div class="empty-state">登录学生账号后显示已发布作业。</div>`;
    }
    if (el.studentCourses) {
        el.studentCourses.innerHTML = `<div class="empty-state">登录学生账号后显示已选课程。</div>`;
    }
    if (el.studentSummaries) {
        el.studentSummaries.innerHTML = `<div class="empty-state">最近提交记录会显示在这里。</div>`;
    }
    if (el.submissionDetail) {
        el.submissionDetail.className = "detail-panel empty-state";
        el.submissionDetail.textContent = "选择一条提交记录后，这里会显示编译结果、运行信息和每个测试用例的通过情况。";
    }
    if (el.studentAiDiagnosisPanel) {
        el.studentAiDiagnosisPanel.className = "detail-panel empty-state";
        el.studentAiDiagnosisPanel.textContent = "发起 AI 辅助分析后，这里会显示问题概述、修改建议和知识点提示。";
    }
}

async function refreshSelectedCourseEnrollments() {
    if (!el.courseEnrollmentList) {
        return;
    }
    const courseId = state.selectedCourseId;
    if (!courseId) {
        state.selectedCourseEnrollments = [];
        state.selectedAvailableStudentId = null;
        state.selectedEnrolledStudentId = null;
        if (el.selectedCourseTitle) {
            el.selectedCourseTitle.textContent = "选择课程后管理选课";
        }
        renderAvailableStudents([]);
        renderCourseEnrollments([]);
        return;
    }
    try {
        const enrollments = await api(`/api/courses/${courseId}/enrollments`);
        state.selectedCourseEnrollments = enrollments;
        const selectedCourse = state.courses.find((course) => String(course.id) === String(courseId));
        if (el.selectedCourseTitle) {
            el.selectedCourseTitle.textContent = selectedCourse
                ? `${selectedCourse.code} · ${selectedCourse.name}（${selectedCourse.className || "未设置班级"}）`
                : "当前课程";
        }
        const enrolledIds = new Set(enrollments.map((item) => String(item.studentId)));
        renderAvailableStudents(state.students.filter((student) => !enrolledIds.has(String(student.id))));
        renderCourseEnrollments(enrollments);
    } catch (error) {
        state.selectedCourseEnrollments = [];
        if (el.selectedCourseTitle) {
            el.selectedCourseTitle.textContent = "选择课程后管理选课";
        }
        renderAvailableStudents([]);
        renderCourseEnrollments([]);
        notify(el.dashboardMessage, error.message, "error", "加载选课失败");
    }
}

function statCard(label, value, hint = "系统数据") {
    return `<article class="stat-card"><span class="muted">${label}</span><strong>${value}</strong><small>${hint}</small></article>`;
}

function statMiniCard(label, value) {
    return `
        <article class="statistics-mini-card">
            <span>${label}</span>
            <strong>${value}</strong>
        </article>
    `;
}

function setMessage(target, message, type = "neutral") {
    if (!target) {
        return;
    }
    target.textContent = message;
    target.classList.remove("is-error", "is-success");
    if (type === "error") {
        target.classList.add("is-error");
    }
    if (type === "success") {
        target.classList.add("is-success");
    }
}

function notify(target, message, type = "neutral", title = "") {
    setMessage(target, message, type);
    showToast(message, type === "neutral" ? "info" : type, title || defaultToastTitle(type));
}

function showToast(message, type = "info", title = "") {
    if (!el.toastViewport) {
        return;
    }
    const toast = document.createElement("div");
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `<strong>${escapeHtml(title || defaultToastTitle(type))}</strong><span>${escapeHtml(message)}</span>`;
    el.toastViewport.appendChild(toast);
    window.setTimeout(() => toast.remove(), 3200);
}

function defaultToastTitle(type) {
    return { success: "操作成功", error: "操作失败", info: "操作提示", neutral: "操作提示" }[type] || "操作提示";
}

function translateStatus(status) {
    return {
        DRAFT: "草稿",
        PUBLISHED: "已发布",
        CLOSED: "已关闭",
        ACCEPTED: "通过",
        PARTIAL_ACCEPTED: "部分通过",
        FAILED: "未通过",
        COMPILE_ERROR: "编译错误",
        RUNTIME_ERROR: "运行错误",
        TIME_LIMIT_EXCEEDED: "超时",
        PENDING: "待评测"
    }[status] || status;
}

function translateRole(role) {
    return {
        ADMIN: "管理员",
        TEACHER: "教师",
        STUDENT: "学生"
    }[role] || role;
}

function statusClass(status) {
    return `status-${String(status || "").toLowerCase()}`;
}

function routeForRole(role) {
    if (role === "ADMIN") {
        return ROUTES.admin;
    }
    return role === "TEACHER" ? ROUTES.teacher : ROUTES.student;
}

function isPrivateRoute(route) {
    return route === ROUTES.admin || route === ROUTES.teacher || route === ROUTES.student;
}

function navigate(route, options = {}) {
    const hash = `#/${route}`;
    if (options.replace) {
        history.replaceState(null, "", hash);
        handleRouteChange();
        return;
    }
    if (window.location.hash === hash) {
        handleRouteChange();
        return;
    }
    window.location.hash = `/${route}`;
}

function focusSubmissionEditor() {
    const editorShell = el.sourceCodeInput.closest(".editor-shell");
    (editorShell || el.sourceCodeInput).scrollIntoView({ behavior: "smooth", block: "start" });
    el.sourceCodeInput.focus();
}

function startSubmissionPolling(submissionId, teacherMode) {
    stopSubmissionPolling();
    state.submissionPollAttempts = 0;
    state.submissionPollTimer = window.setInterval(async () => {
        state.submissionPollAttempts += 1;
        try {
            const submission = await api(`/api/submissions/${submissionId}`);
            renderSubmissionDetail(teacherMode ? el.teacherSubmissionDetail : el.submissionDetail, submission);
            if (submission.status !== "PENDING") {
                stopSubmissionPolling();
                if (state.profile?.role === "TEACHER") {
                    await loadTeacherDashboard("评测已完成，结果已刷新。", true);
                    if (teacherMode && el.submissionAssignmentSelect?.value) {
                        await loadTeacherSubmissionsWithOptions({ silent: true });
                    }
                } else {
                    await loadStudentDashboard("评测已完成，结果已刷新。", true);
                }
                showToast("后台评测已完成。", "success", "评测完成");
                return;
            }
            if (state.submissionPollAttempts >= 40) {
                stopSubmissionPolling();
                showToast("评测超时，请手动刷新查看结果。", "error", "评测超时");
            }
        } catch (error) {
            stopSubmissionPolling();
        }
    }, 3000);
}

function stopSubmissionPolling() {
    if (state.submissionPollTimer) {
        window.clearInterval(state.submissionPollTimer);
        state.submissionPollTimer = null;
    }
    state.submissionPollAttempts = 0;
}

function normalizeDateTime(value) {
    return value ? `${value}:00` : value;
}

function toNullableNumber(value) {
    if (value === null || value === undefined || value === "") {
        return null;
    }
    return Number(value);
}

function formatDateTime(value) {
    if (!value) {
        return "未提供";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }
    return new Intl.DateTimeFormat("zh-CN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    }).format(date);
}

function formatLoginStatus(expiresAt) {
    if (!expiresAt) {
        return "登录状态正常";
    }
    const expiryDate = new Date(expiresAt);
    if (Number.isNaN(expiryDate.getTime())) {
        return "登录状态正常";
    }
    const remainingMs = expiryDate.getTime() - Date.now();
    if (remainingMs <= 0) {
        return "登录已过期，请重新登录";
    }
    const remainingMinutes = Math.ceil(remainingMs / 60000);
    if (remainingMinutes <= 30) {
        return `登录即将过期，约 ${remainingMinutes} 分钟后失效`;
    }
    return "登录状态正常";
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function escapeMultilineText(value) {
    return escapeHtml(value).replaceAll("\n", "<br>");
}

async function downloadAssignmentGrades(assignmentId) {
    const headers = {};
    if (state.auth?.token) {
        headers.Authorization = `Bearer ${state.auth.token}`;
    }
    const response = await fetch(`/api/assignments/${assignmentId}/grades/export`, { headers });
    if (!response.ok) {
        const text = await response.text();
        const data = text ? safeParseJson(text) : null;
        throw new Error(data?.message || "导出失败，请稍后再试。");
    }

    const blob = await response.blob();
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = objectUrl;
    link.download = parseFilename(response.headers.get("Content-Disposition")) || `assignment-${assignmentId}-grades.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(objectUrl);
}

function parseFilename(contentDisposition) {
    if (!contentDisposition) {
        return "";
    }
    const match = contentDisposition.match(/filename="([^"]+)"/i);
    return match ? match[1] : "";
}

async function api(url, options = {}, requiresAuth = true) {
    const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
    if (requiresAuth && state.auth?.token) {
        headers.Authorization = `Bearer ${state.auth.token}`;
    }
    const response = await fetch(url, { ...options, headers });
    if (response.status === 204) {
        return null;
    }
    const text = await response.text();
    const data = text ? safeParseJson(text) : null;
    if (!response.ok) {
        throw new Error(data?.message || "请求失败，请稍后再试。");
    }
    return data;
}

function safeParseJson(text) {
    try {
        return JSON.parse(text);
    } catch (error) {
        return null;
    }
}

function byId(id) {
    return document.getElementById(id);
}

function readJson(key) {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
}

function writeAuth() {
    if (state.auth) {
        localStorage.setItem("autograding-auth", JSON.stringify(state.auth));
    } else {
        localStorage.removeItem("autograding-auth");
    }
}
