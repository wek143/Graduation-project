const ROUTES = {
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
    latestSummaries: [],
    selectedSubmission: null,
    adminLists: createAdminListState(),
    activePortalModule: {
        teacher: null,
        student: null
    },
    route: ROUTES.login,
    submissionPollTimer: null,
    submissionPollAttempts: 0
};

const el = {
    authView: byId("authView"),
    appView: byId("appView"),
    publicNav: byId("publicNav"),
    privateNav: byId("privateNav"),
    navLoginButton: byId("navLoginButton"),
    navRegisterButton: byId("navRegisterButton"),
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
    el.navLoginButton?.addEventListener("click", () => navigate(ROUTES.login));
    el.navRegisterButton?.addEventListener("click", () => navigate(ROUTES.register));
    el.loginTabButton?.addEventListener("click", () => navigate(ROUTES.register));
    el.registerTabButton?.addEventListener("click", () => navigate(ROUTES.login));
    el.loginForm?.addEventListener("submit", onLogin);
    el.registerForm?.addEventListener("submit", onRegister);
    const pwdToggle = document.getElementById("loginPasswordToggle");
    if (pwdToggle && el.loginPasswordInput) {
        pwdToggle.addEventListener("click", () => {
            const isText = el.loginPasswordInput.type === "text";
            el.loginPasswordInput.type = isText ? "password" : "text";
            pwdToggle.querySelector(".eye-open").classList.toggle("hidden", !isText);
            pwdToggle.querySelector(".eye-closed").classList.toggle("hidden", isText);
        });
    }
    el.logoutButton?.addEventListener("click", onLogout);
    el.refreshDashboardButton?.addEventListener("click", hydrateSession);
    el.adminAiSettingsForm?.addEventListener("submit", onSaveAiSettings);
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
    bindAdminCourseForm();
    bindPortalNav(section, "admin");
    section.dataset.portalReady = "true";
}

function initializeTeacherPortal() {
    const section = el.teacherSection;
    if (!section || section.dataset.portalReady === "true") {
        return;
    }

    const overview = el.overviewCards;
    const createPanel = el.assignmentForm?.closest(".panel");
    const libraryPanel = el.teacherAssignments?.closest(".panel");
    const submissionsPanel = el.teacherSubmissions?.closest(".panel");
    const submissionControls = el.submissionAssignmentSelect?.closest(".inline-form");
    const statsPanel = el.assignmentStatistics?.closest(".panel");
    const detailPanel = el.teacherSubmissionDetail?.closest(".panel");
    const importPanel = el.importUsersForm?.closest(".panel");
    const auditPanel = el.auditLogList?.closest(".panel");
    const joinPanel = byId("teacherJoinPanel");
    const profilePanel = byId("teacherProfilePanel");

    const shell = document.createElement("div");
    shell.className = "portal-shell";
    shell.innerHTML = `
        <aside class="portal-sidebar">
            <div class="portal-sidebar-title">教师工作台</div>
            <button type="button" class="portal-nav-item" data-portal-role="teacher" data-module-target="">工作台</button>
            <button type="button" class="portal-nav-item" data-portal-role="teacher" data-module-target="teacher-join">加入班级</button>
            <button type="button" class="portal-nav-item" data-portal-role="teacher" data-module-target="teacher-create">发布作业</button>
            <button type="button" class="portal-nav-item" data-portal-role="teacher" data-module-target="teacher-library">作业管理</button>
            <button type="button" class="portal-nav-item" data-portal-role="teacher" data-module-target="teacher-submissions">提交记录</button>
            <button type="button" class="portal-nav-item" data-portal-role="teacher" data-module-target="teacher-statistics">统计分析</button>
            <button type="button" class="portal-nav-item" data-portal-role="teacher" data-module-target="teacher-import">批量导入</button>
            <button type="button" class="portal-nav-item" data-portal-role="teacher" data-module-target="teacher-profile">信息修改</button>
        </aside>
        <div class="portal-main"></div>
    `;
    const main = shell.querySelector(".portal-main");

    const homePanel = createPortalPanel("teacher-home");
    homePanel.innerHTML = `
        <div class="portal-home-card">
            <div class="portal-home-copy">
                <h3>教师工作台</h3>
                <p>查看课程与作业的最新动态。</p>
            </div>
            <div id="teacherClock" class="portal-home-clock">--:--:--</div>
        </div>
    `;
    const dashGrid = document.createElement("div");
    dashGrid.className = "teacher-dashboard-grid";
    dashGrid.innerHTML = `
        <div class="portal-panel-card">
            <div class="portal-panel-head"><h3>待截止作业</h3><span class="panel-kicker">近期截止</span></div>
            <div id="teacherHomeDue" class="stack-list"></div>
        </div>
        <div class="portal-panel-card">
            <div class="portal-panel-head"><h3>最近提交</h3><span class="panel-kicker">按时间排序</span></div>
            <div id="teacherHomeActivity" class="stack-list"></div>
        </div>
    `;
    homePanel.appendChild(dashGrid);
    main.appendChild(homePanel);

    movePanelToPortal(main, "teacher-join", joinPanel);
    movePanelToPortal(main, "teacher-create", createPanel);
    moveContentToPortal(main, "teacher-library", "作业与用例管理", el.teacherAssignments);
    moveContentToPortal(main, "teacher-submissions", "查看提交", submissionControls, el.teacherSubmissions, el.teacherSubmissionDetail);
    movePanelToPortal(main, "teacher-statistics", statsPanel);
    movePanelToPortal(main, "teacher-import", importPanel);
    movePanelToPortal(main, "teacher-profile", profilePanel);

    section.prepend(shell);
    cleanupEmptyContainers(section);
    libraryPanel?.remove();
    submissionsPanel?.remove();
    detailPanel?.remove();
    auditPanel?.remove();
    bindPortalNav(section, "teacher");
    bindJoinPanel("teacher");
    bindProfileForm("teacher");
    section.dataset.portalReady = "true";
}

function initializeStudentPortal() {
    const section = el.studentSection;
    if (!section || section.dataset.portalReady === "true") {
        return;
    }

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
            <button type="button" class="portal-nav-item" data-portal-role="student" data-module-target="student-join">加入班级</button>
            <button type="button" class="portal-nav-item" data-portal-role="student" data-module-target="student-courses">我的课程</button>
            <button type="button" class="portal-nav-item" data-portal-role="student" data-module-target="student-assignments">已发布作业</button>
            <button type="button" class="portal-nav-item" data-portal-role="student" data-module-target="student-submit">代码提交</button>
            <button type="button" class="portal-nav-item" data-portal-role="student" data-module-target="student-results">最近提交</button>
            <button type="button" class="portal-nav-item" data-portal-role="student" data-module-target="student-detail">评测详情</button>
            <button type="button" class="portal-nav-item" data-portal-role="student" data-module-target="student-profile">信息修改</button>
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
    const joinPanel = byId("studentJoinPanel");
    const profilePanel = byId("studentProfilePanel");

    main.appendChild(homePanel);

    movePanelToPortal(main, "student-join", joinPanel);
    movePanelToPortal(main, "student-courses", coursesPanel);
    movePanelToPortal(main, "student-assignments", assignmentsPanel);
    movePanelToPortal(main, "student-submit", submitPanel);
    movePanelToPortal(main, "student-results", summariesPanel);
    movePanelToPortal(main, "student-detail", detailPanel);
    movePanelToPortal(main, "student-profile", profilePanel);

    section.prepend(shell);
    cleanupEmptyContainers(section);
    bindPortalNav(section, "student");
    bindJoinPanel("student");
    bindProfileForm("student");
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
            const target = button.dataset.moduleTarget || null;
            state.activePortalModule[role] = target;
            renderPortalModules();
            if (target === `${role}-join`) {
                loadJoinCourses(role);
            }
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
    return Object.values(ROUTES).includes(raw) ? raw : ROUTES.login;
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
    navigate(ROUTES.login);
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
    const [overview, assignments, assignmentStats, courseStats, courses] = await Promise.all([
        api("/api/users/overview"),
        api("/api/assignments"),
        api("/api/assignments/statistics/overview"),
        api("/api/courses/statistics/overview"),
        api("/api/courses")
    ]);

    state.assignments = assignments;
    state.courses = courses;
    state.teacherStatistics.courseStats = courseStats;
    state.teacherStatistics.assignmentStats = assignmentStats;

    if (el.overviewCards) el.overviewCards.innerHTML = "";

    renderTeacherHomeDue(assignments);
    renderTeacherHomeActivity(assignmentStats);
    renderTeacherAssignments(assignments);
    renderTeacherStatistics();
    fillEntitySelect(el.assignmentCourseSelect, courses, "请选择课程", (course) => `${course.code ? course.code + " · " : ""}${course.name}`);
    fillEntitySelect(el.submissionAssignmentSelect, assignments, "选择作业查看提交", (assignment) => assignment.title);

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
function renderTeacherHomeDue(assignments) {
    const container = byId("teacherHomeDue");
    if (!container) return;
    const now = Date.now();
    const upcoming = assignments
        .filter((a) => a.status === "PUBLISHED" && a.deadline && new Date(a.deadline).getTime() > now)
        .sort((a, b) => new Date(a.deadline) - new Date(b.deadline))
        .slice(0, 6);
    if (!upcoming.length) {
        container.innerHTML = `<div class="empty-state">暂无即将截止的作业。</div>`;
        return;
    }
    container.innerHTML = upcoming.map((a) => {
        const ms = new Date(a.deadline).getTime() - now;
        const days = Math.ceil(ms / 86400000);
        const urgency = days <= 1 ? "danger" : days <= 3 ? "warning" : "info";
        const urgencyLabel = days <= 1 ? "今日截止" : days <= 3 ? `${days} 天后` : `${days} 天后`;
        return `
        <article class="stack-item">
            <div class="inline-header">
                <h4>${escapeHtml(a.title)}</h4>
                <span class="badge badge-${urgency}">${urgencyLabel}</span>
            </div>
            <div class="stack-meta">
                <span>截止：${formatDateTime(a.deadline)}</span>
                <span>最多提交 ${a.maxSubmissions} 次</span>
            </div>
        </article>`;
    }).join("");
}

function renderTeacherHomeActivity(assignmentStats) {
    const container = byId("teacherHomeActivity");
    if (!container) return;
    const items = [...(assignmentStats || [])]
        .filter((i) => i.totalSubmissions > 0)
        .sort((a, b) => (b.lastSubmittedAt ? new Date(b.lastSubmittedAt) : 0) - (a.lastSubmittedAt ? new Date(a.lastSubmittedAt) : 0))
        .slice(0, 6);
    container.innerHTML = items.length ? items.map((item) => `
        <article class="stack-item">
            <div class="inline-header">
                <h4>${escapeHtml(item.assignmentTitle)}</h4>
                <span class="pill ${statusClass(item.assignmentStatus)}">${translateStatus(item.assignmentStatus)}</span>
            </div>
            <div class="stack-meta">
                <span>提交：${item.totalSubmissions}</span>
                <span>参与学生：${item.distinctStudentCount}</span>
                <span>平均分：${item.averageScore.toFixed(1)}</span>
            </div>
        </article>
    `).join("") : `<div class="empty-state">暂无提交记录。</div>`;
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

function generateSemesters() {
    const semesters = [];
    for (let y = 2026; y >= 2022; y--) {
        semesters.push(`${y}~${y + 1}第二学期`);
        semesters.push(`${y}~${y + 1}第一学期`);
    }
    return semesters;
}

function fillSemesterSelect(selectEl, includeAll = true) {
    if (!selectEl) return;
    const current = selectEl.value;
    const options = generateSemesters().map((s) => `<option value="${escapeHtml(s)}">${escapeHtml(s)}</option>`).join("");
    selectEl.innerHTML = (includeAll ? `<option value="">全部学期</option>` : "") + options;
    if (current) selectEl.value = current;
}

function renderAdminCourses(items) {
    if (!el.adminCourseList) return;
    el.adminCourseList.innerHTML = items.length ? items.map((item) => `
        <article class="stack-item">
            <div class="inline-header">
                <h4>${escapeHtml(item.name)}${item.code ? ` <small class="muted">(${escapeHtml(item.code)})</small>` : ""}</h4>
                <span class="pill ${item.active ? "status-published" : "status-closed"}">${item.active ? "进行中" : "已停用"}</span>
            </div>
            <div class="stack-meta">
                <span>学期：${escapeHtml(item.term)}</span>
                <span>班级：${escapeHtml(item.className || "-")}</span>
                <span>任课教师：${item.teacherName ? escapeHtml(item.teacherName) : "<em>待认领</em>"}</span>
            </div>
            <div class="stack-actions">
                <button class="btn btn-small btn-secondary" data-admin-course-edit="${item.id}">编辑</button>
                <button class="btn btn-small btn-danger" data-admin-course-delete="${item.id}">删除</button>
            </div>
        </article>
    `).join("") : `<div class="empty-state">暂无课程班级数据。</div>`;

    el.adminCourseList.querySelectorAll("[data-admin-course-edit]").forEach((btn) => {
        btn.addEventListener("click", () => startAdminCourseEdit(Number(btn.dataset.adminCourseEdit), items));
    });
    el.adminCourseList.querySelectorAll("[data-admin-course-delete]").forEach((btn) => {
        btn.addEventListener("click", () => onAdminCourseDelete(Number(btn.dataset.adminCourseDelete)));
    });
}

function startAdminCourseEdit(courseId, items) {
    const item = items.find((c) => c.id === courseId);
    if (!item) return;
    const form = byId("adminCourseForm");
    const createForm = byId("adminCourseCreateForm");
    const submitBtn = byId("adminCourseSubmitBtn");
    if (!form || !createForm || !submitBtn) return;
    form.elements.name.value = item.name || "";
    form.elements.code.value = item.code || "";
    form.elements.className.value = item.className || "";
    fillSemesterSelect(byId("adminCourseTerm"), false);
    byId("adminCourseTerm").value = item.term || "";
    submitBtn.textContent = "保存修改";
    form.dataset.editId = courseId;
    createForm.classList.remove("hidden");
    createForm.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

async function onAdminCourseDelete(courseId) {
    if (!confirm("确认删除该课程班级？此操作不可撤销。")) return;
    try {
        await api(`/api/admin/courses/${courseId}`, { method: "DELETE" });
        showToast("课程班级已删除。", "success", "删除成功");
        loadAdminListPage("courses", 0);
    } catch (error) {
        showToast(error.message, "error", "删除失败");
    }
}

function bindAdminCourseForm() {
    const createBtn = byId("adminCourseCreateBtn");
    const cancelBtn = byId("adminCourseCancelBtn");
    const createForm = byId("adminCourseCreateForm");
    const form = byId("adminCourseForm");
    const termSelect = byId("adminCourseTerm");

    if (!createBtn || !form) return;
    fillSemesterSelect(termSelect, false);

    createBtn.addEventListener("click", () => {
        form.reset();
        delete form.dataset.editId;
        const submitBtn = byId("adminCourseSubmitBtn");
        if (submitBtn) submitBtn.textContent = "创建课程班级";
        fillSemesterSelect(termSelect, false);
        createForm?.classList.toggle("hidden");
    });
    cancelBtn?.addEventListener("click", () => {
        form.reset();
        delete form.dataset.editId;
        createForm?.classList.add("hidden");
    });
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const data = new FormData(form);
        const body = {
            name: data.get("name"),
            code: data.get("code") || null,
            term: data.get("term"),
            className: data.get("className")
        };
        try {
            const editId = form.dataset.editId;
            if (editId) {
                await api(`/api/admin/courses/${editId}`, { method: "PUT", body: JSON.stringify({ ...body, active: true }) });
                showToast("课程班级已更新。", "success", "保存成功");
            } else {
                await api("/api/admin/courses", { method: "POST", body: JSON.stringify(body) });
                showToast("课程班级已创建。", "success", "创建成功");
            }
            form.reset();
            delete form.dataset.editId;
            createForm?.classList.add("hidden");
            loadAdminListPage("courses", 0);
        } catch (error) {
            showToast(error.message, "error", "操作失败");
        }
    });
}

let joinCourseCache = [];

async function loadJoinCourses(role) {
    const termFilterId = role === "teacher" ? "teacherJoinTermFilter" : "studentJoinTermFilter";
    const searchId = role === "teacher" ? "teacherJoinSearch" : "studentJoinSearch";
    const listId = role === "teacher" ? "teacherJoinList" : "studentJoinList";
    const term = byId(termFilterId)?.value || "";
    const keyword = (byId(searchId)?.value || "").toLowerCase();
    const container = byId(listId);
    if (!container) return;
    try {
        const url = term ? `/api/courses/available?term=${encodeURIComponent(term)}` : "/api/courses/available";
        joinCourseCache = await api(url);
        renderJoinCourses(role, joinCourseCache, keyword);
    } catch (error) {
        container.innerHTML = `<div class="empty-state">加载失败：${escapeHtml(error.message)}</div>`;
    }
}

function renderJoinCourses(role, courses, keyword = "") {
    const listId = role === "teacher" ? "teacherJoinList" : "studentJoinList";
    const container = byId(listId);
    if (!container) return;

    const myCourses = state.courses || [];
    const myIds = new Set(myCourses.map((c) => String(c.id)));

    const filtered = keyword
        ? courses.filter((c) => c.name.toLowerCase().includes(keyword) || (c.code || "").toLowerCase().includes(keyword))
        : courses;

    if (!filtered.length) {
        container.innerHTML = `<div class="empty-state">暂无可加入的课程班级。</div>`;
        return;
    }

    container.innerHTML = filtered.map((c) => {
        const joined = myIds.has(String(c.id));
        const teacherTaken = c.teacherName && role === "teacher" && !joined;
        let statusBadge = "";
        let actionBtn = "";
        if (joined) {
            statusBadge = `<span class="pill status-published">已加入</span>`;
            actionBtn = `<button class="btn btn-small btn-ghost" data-leave-course="${c.id}">退出</button>`;
        } else if (teacherTaken) {
            statusBadge = `<span class="pill status-closed">已有教师</span>`;
            actionBtn = `<button class="btn btn-small btn-ghost" disabled>无法加入</button>`;
        } else {
            actionBtn = `<button class="btn btn-small btn-primary" data-join-course="${c.id}">加入</button>`;
        }
        return `
        <div class="join-course-card">
            <div class="join-course-top">
                <div>
                    <strong>${escapeHtml(c.name)}</strong>
                    ${c.code ? `<small class="muted"> · ${escapeHtml(c.code)}</small>` : ""}
                </div>
                ${statusBadge}
            </div>
            <div class="join-course-meta">
                <span>${escapeHtml(c.term)}</span>
                <span>${escapeHtml(c.className || "")}</span>
                <span>教师：${c.teacherName ? escapeHtml(c.teacherName) : "待认领"}</span>
            </div>
            <div class="join-course-action">${actionBtn}</div>
        </div>`;
    }).join("");

    container.querySelectorAll("[data-join-course]").forEach((btn) => {
        btn.addEventListener("click", async () => {
            try {
                await api(`/api/courses/${btn.dataset.joinCourse}/join`, { method: "POST" });
                showToast("已成功加入课程班级。", "success", "加入成功");
                if (role === "teacher") {
                    await loadTeacherDashboard("", true);
                } else {
                    await loadStudentDashboard("", true);
                }
                loadJoinCourses(role);
            } catch (error) {
                showToast(error.message, "error", "加入失败");
            }
        });
    });
    container.querySelectorAll("[data-leave-course]").forEach((btn) => {
        btn.addEventListener("click", async () => {
            if (!confirm("确认退出该课程班级？")) return;
            try {
                await api(`/api/courses/${btn.dataset.leaveCourse}/leave`, { method: "DELETE" });
                showToast("已退出课程班级。", "success", "退出成功");
                if (role === "teacher") {
                    await loadTeacherDashboard("", true);
                } else {
                    await loadStudentDashboard("", true);
                }
                loadJoinCourses(role);
            } catch (error) {
                showToast(error.message, "error", "退出失败");
            }
        });
    });
}

function bindJoinPanel(role) {
    const termFilterId = role === "teacher" ? "teacherJoinTermFilter" : "studentJoinTermFilter";
    const searchId = role === "teacher" ? "teacherJoinSearch" : "studentJoinSearch";
    const refreshId = role === "teacher" ? "teacherJoinRefreshBtn" : "studentJoinRefreshBtn";
    fillSemesterSelect(byId(termFilterId), true);
    byId(termFilterId)?.addEventListener("change", () => loadJoinCourses(role));
    byId(searchId)?.addEventListener("input", (e) => renderJoinCourses(role, joinCourseCache, e.target.value.toLowerCase()));
    byId(refreshId)?.addEventListener("click", () => loadJoinCourses(role));
}

function bindProfileForm(role) {
    const formId = role === "teacher" ? "teacherProfileForm" : "studentProfileForm";
    const msgId = role === "teacher" ? "teacherProfileMessage" : "studentProfileMessage";
    const form = byId(formId);
    if (!form) return;

    api("/api/users/me").then((profile) => {
        const fullNameInput = form.elements.fullName;
        if (fullNameInput) fullNameInput.value = profile.fullName || "";
        if (role === "student") {
            const classNameInput = form.elements.className;
            if (classNameInput) classNameInput.value = profile.className || "";
        }
    }).catch(() => {});

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const data = new FormData(form);
        const body = {};
        const fullName = data.get("fullName");
        const className = data.get("className");
        const oldPassword = data.get("oldPassword");
        const newPassword = data.get("newPassword");
        if (fullName !== null) body.fullName = fullName;
        if (className !== null) body.className = className;
        if (newPassword) { body.oldPassword = oldPassword; body.newPassword = newPassword; }
        try {
            await api("/api/users/me", { method: "PUT", body: JSON.stringify(body) });
            notify(byId(msgId), "信息已更新。", "success", "保存成功");
            form.elements.oldPassword && (form.elements.oldPassword.value = "");
            form.elements.newPassword && (form.elements.newPassword.value = "");
        } catch (error) {
            notify(byId(msgId), error.message, "error", "保存失败");
        }
    });
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
        if (filters.courseId && String(item.courseId) !== String(filters.courseId)) return false;
        if (!normalizedKeyword) return true;
        return [item.courseCode, item.courseName, item.term]
            .some((v) => String(v || "").toLowerCase().includes(normalizedKeyword));
    });

    const filteredAssignmentStats = assignmentStats.filter((item) => {
        const assignment = assignmentMetaById.get(String(item.assignmentId));
        if (filters.courseId && String(assignment?.courseId || "") !== String(filters.courseId)) return false;
        if (filters.status && String(item.assignmentStatus) !== String(filters.status)) return false;
        if (!normalizedKeyword) return true;
        return [item.assignmentTitle, assignment?.courseCode, assignment?.courseName]
            .some((v) => String(v || "").toLowerCase().includes(normalizedKeyword));
    });

    const summary = {
        courseCount: filteredCourseStats.length,
        assignmentCount: filteredAssignmentStats.length,
        totalSubmissions: filteredAssignmentStats.reduce((s, i) => s + (i.totalSubmissions || 0), 0),
        averageScore: filteredAssignmentStats.length
            ? filteredAssignmentStats.reduce((s, i) => s + (i.averageScore || 0), 0) / filteredAssignmentStats.length
            : 0
    };

    const hasCourseStats = courseStats.length > 0;
    const hasAssignmentStats = assignmentStats.length > 0;
    if (!hasCourseStats && !hasAssignmentStats) {
        el.assignmentStatistics.innerHTML = `<div class="empty-state">暂无统计数据。</div>`;
        return;
    }

    const toolbar = `
        <div class="stat-page-header">
            <div class="stat-filter-bar">
                <div class="stat-filter-search">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                    <input id="statisticsKeywordInput" type="search" placeholder="搜索课程或作业" value="${escapeHtml(filters.keyword)}">
                </div>
                <select id="statisticsCourseFilter" class="stat-filter-select">
                    <option value="">全部课程</option>
                    ${courseOptions}
                </select>
                <select id="statisticsStatusFilter" class="stat-filter-select">
                    <option value="">全部状态</option>
                    <option value="DRAFT" ${filters.status === "DRAFT" ? "selected" : ""}>草稿</option>
                    <option value="PUBLISHED" ${filters.status === "PUBLISHED" ? "selected" : ""}>已发布</option>
                    <option value="CLOSED" ${filters.status === "CLOSED" ? "selected" : ""}>已关闭</option>
                </select>
                <button id="statisticsClearButton" type="button" class="btn btn-small btn-ghost">清空</button>
            </div>
            <div class="stat-kpi-row">
                <div class="stat-kpi-card stat-kpi-blue">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>
                    <div><strong>${summary.courseCount}</strong><span>课程数</span></div>
                </div>
                <div class="stat-kpi-card stat-kpi-purple">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
                    <div><strong>${summary.assignmentCount}</strong><span>作业数</span></div>
                </div>
                <div class="stat-kpi-card stat-kpi-teal">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
                    <div><strong>${summary.totalSubmissions}</strong><span>提交总量</span></div>
                </div>
                <div class="stat-kpi-card stat-kpi-orange">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="6"/><path d="M15.477 12.89L17 22l-5-3-5 3 1.523-9.11"/></svg>
                    <div><strong>${summary.averageScore.toFixed(1)}</strong><span>平均分</span></div>
                </div>
            </div>
        </div>
    `;

    const courseSection = hasCourseStats ? `
        <div class="stat-section">
            <div class="stat-section-head">
                <h4>课程统计</h4>
                <span>共 ${filteredCourseStats.length} 门</span>
            </div>
            <div class="stat-course-grid">
                ${filteredCourseStats.length ? filteredCourseStats.map((item) => `
                    <div class="stat-course-card">
                        <div class="stat-course-card-top">
                            <div class="stat-course-title">
                                <strong>${escapeHtml(item.courseCode)}</strong>
                                <span>${escapeHtml(item.courseName)}</span>
                            </div>
                            <span class="pill ${item.active ? "status-published" : "status-closed"}">${item.active ? "进行中" : "已停用"}</span>
                        </div>
                        <div class="stat-course-meta">
                            <span>${escapeHtml(item.term)}</span>
                            ${item.className ? `<span>${escapeHtml(item.className)}</span>` : ""}
                        </div>
                        <div class="stat-course-nums">
                            <div class="stat-course-num">
                                <strong>${item.enrollmentCount}</strong>
                                <span>选课人数</span>
                            </div>
                            <div class="stat-course-num">
                                <strong>${item.publishedAssignmentCount}<small>/${item.assignmentCount}</small></strong>
                                <span>已发布作业</span>
                            </div>
                            <div class="stat-course-num">
                                <strong>${item.totalSubmissions}</strong>
                                <span>提交总量</span>
                            </div>
                            <div class="stat-course-num stat-course-num-score">
                                <strong>${item.averageScore.toFixed(1)}</strong>
                                <span>平均分</span>
                            </div>
                        </div>
                    </div>
                `).join("") : `<div class="empty-state">当前筛选下没有课程统计。</div>`}
            </div>
        </div>
    ` : "";

    const assignmentSection = hasAssignmentStats ? `
        <div class="stat-section">
            <div class="stat-section-head">
                <h4>作业统计</h4>
                <span>共 ${filteredAssignmentStats.length} 个</span>
            </div>
            <div class="stat-assignment-list">
                ${filteredAssignmentStats.length ? filteredAssignmentStats.map((item) => {
                    const assignment = assignmentMetaById.get(String(item.assignmentId));
                    const score = item.averageScore || 0;
                    const passRate = item.totalSubmissions > 0
                        ? Math.round((item.distinctStudentCount / item.totalSubmissions) * 100)
                        : 0;
                    const scoreWidth = Math.min(100, Math.round(score));
                    const scoreColor = score >= 80 ? "var(--success)" : score >= 60 ? "var(--warning)" : "var(--danger)";
                    return `
                    <div class="stat-assignment-card">
                        <div class="stat-assignment-top">
                            <div class="stat-assignment-title">
                                <h4>${escapeHtml(item.assignmentTitle)}</h4>
                                <span class="stat-assignment-course">${escapeHtml(assignment?.courseCode ? `${assignment.courseCode} · ${assignment.courseName}` : "-")}</span>
                            </div>
                            <span class="pill ${statusClass(item.assignmentStatus)}">${translateStatus(item.assignmentStatus)}</span>
                        </div>
                        <div class="stat-assignment-nums">
                            <div class="stat-num-item">
                                <span>提交次数</span>
                                <strong>${item.totalSubmissions}</strong>
                            </div>
                            <div class="stat-num-item">
                                <span>参与学生</span>
                                <strong>${item.distinctStudentCount}</strong>
                            </div>
                            <div class="stat-num-item">
                                <span>平均分</span>
                                <strong style="color:${scoreColor}">${score.toFixed(1)}</strong>
                            </div>
                        </div>
                        <div class="stat-score-bar-wrap">
                            <div class="stat-score-bar-track">
                                <div class="stat-score-bar-fill" style="width:${scoreWidth}%;background:${scoreColor}"></div>
                            </div>
                            <span class="stat-score-bar-label">${score.toFixed(1)} / 100</span>
                        </div>
                        <div class="stat-assignment-actions">
                            <button class="btn btn-small btn-primary" data-stat-submission-assignment-id="${item.assignmentId}">查看提交</button>
                            <button class="btn btn-small btn-secondary" data-grade-export-id="${item.assignmentId}">导出成绩 CSV</button>
                        </div>
                    </div>
                `;
                }).join("") : `<div class="empty-state">当前筛选下没有作业统计。</div>`}
            </div>
        </div>
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
    if (el.overviewCards) {
        el.overviewCards.innerHTML = "";
    }
    const homeDue = byId("teacherHomeDue");
    if (homeDue) homeDue.innerHTML = `<div class="empty-state">登录教师账号后显示待截止作业。</div>`;
    const homeActivity = byId("teacherHomeActivity");
    if (homeActivity) homeActivity.innerHTML = `<div class="empty-state">登录教师账号后显示提交记录。</div>`;
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
