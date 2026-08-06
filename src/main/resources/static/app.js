let spaces = [
  {
    id: 1, title: "Box seguro na Vila Mariana", city: "São Paulo", district: "Vila Mariana", type: "Box", size: 8,
    price: 389, rating: 4.9, reviews: 47, distance: 2.4, verified: true, available: true,
    features: ["Câmeras 24h", "Acesso controlado", "Coberto", "Seguro contra incêndio"],
    description: "Box individual, seco e ventilado, ideal para móveis, caixas e itens de mudança. Acesso mediante agendamento todos os dias.",
    owner: "Marina Costa", ownerSince: "Proprietária verificada desde 2024", toneA: "#b8d9cb", toneB: "#527c6b"
  },
  {
    id: 2, title: "Depósito prático em Pinheiros", city: "São Paulo", district: "Pinheiros", type: "Depósito", size: 14,
    price: 620, rating: 4.8, reviews: 31, distance: 4.8, verified: true, available: true,
    features: ["Acesso 24h", "Alarme", "Carga e descarga", "Iluminação"],
    description: "Espaço amplo e de fácil acesso para pequenos estoques, equipamentos ou uma mudança completa. Área monitorada e entrada independente.",
    owner: "Lucas Almeida", ownerSince: "Proprietário verificado desde 2025", toneA: "#e8cda6", toneB: "#b8784f"
  },
  {
    id: 3, title: "Garagem coberta no Tatuapé", city: "São Paulo", district: "Tatuapé", type: "Garagem", size: 11,
    price: 310, rating: 4.7, reviews: 19, distance: 7.1, verified: true, available: true,
    features: ["Portaria", "Coberto", "Fácil acesso", "Área residencial"],
    description: "Garagem coberta em condomínio tranquilo, excelente para bicicleta, moto, caixas e objetos sazonais de médio porte.",
    owner: "Beatriz Nunes", ownerSince: "Proprietária verificada desde 2025", toneA: "#c7d5e8", toneB: "#6f82a3"
  },
  {
    id: 4, title: "Mini galpão em Santo Amaro", city: "São Paulo", district: "Santo Amaro", type: "Galpão", size: 32,
    price: 1280, rating: 5.0, reviews: 12, distance: 9.6, verified: true, available: true,
    features: ["Pé-direito alto", "Docas", "CFTV", "Energia elétrica"],
    description: "Mini galpão independente para operação de e-commerce e estoque. Entrada para utilitários e área de carga compartilhada.",
    owner: "Armazéns Leste", ownerSince: "Empresa verificada desde 2023", toneA: "#d8c7b6", toneB: "#8c7161"
  },
  {
    id: 5, title: "Armário individual na Bela Vista", city: "São Paulo", district: "Bela Vista", type: "Armário", size: 3,
    price: 189, rating: 4.6, reviews: 64, distance: 1.8, verified: true, available: true,
    features: ["Lacre individual", "Recepção", "Climatizado", "Acesso diário"],
    description: "Armário privativo em ambiente climatizado para documentos, coleções e pequenos volumes. No coração da cidade.",
    owner: "StowBox Centro", ownerSince: "Empresa verificada desde 2022", toneA: "#d6d8b4", toneB: "#8a9362"
  },
  {
    id: 6, title: "Container privativo na Mooca", city: "São Paulo", district: "Mooca", type: "Container", size: 18,
    price: 790, rating: 4.8, reviews: 26, distance: 6.3, verified: true, available: true,
    features: ["Privativo", "Câmeras", "Ventilado", "Estacionamento"],
    description: "Container marítimo adaptado, impermeável e ventilado. Perfeito para estoque comercial ou armazenamento de longo prazo.",
    owner: "Rafael Mello", ownerSince: "Proprietário verificado desde 2024", toneA: "#b9d7d9", toneB: "#4e8589"
  }
];

spaces = [];

const seededBookings = [
  { id: "RSV-2408", spaceId: 2, start: "2026-08-01", months: 3, total: 2015, status: "Confirmada", contract: "CTR-2408" }
];

seededBookings.length = 0;

const store = {
  get(key, fallback) {
    try { return JSON.parse(localStorage.getItem(`stowup-${key}`)) ?? fallback; } catch { return fallback; }
  },
  set(key, value) { localStorage.setItem(`stowup-${key}`, JSON.stringify(value)); }
};

const state = {
  view: "home",
  query: "",
  type: "Todos",
  size: "Todos",
  sort: "relevance",
  favorites: store.get("favorites", []),
  bookings: [],
  userSpaces: store.get("user-spaces", []),
  selectedSpace: null,
  token: store.get("token", null),
  user: store.get("user", null),
  apiOnline: false
};

const app = document.querySelector("#app");
const modalRoot = document.querySelector("#modal-root");
const toastRoot = document.querySelector("#toast-root");

const money = value => Number(value).toLocaleString("pt-BR", { style: "currency", currency: "BRL", maximumFractionDigits: 0 });
const dateBR = value => new Date(`${value}T12:00:00`).toLocaleDateString("pt-BR", { day: "2-digit", month: "short", year: "numeric" });
const escapeHTML = value => String(value).replace(/[&<>'"]/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" }[char]));
const logo = () => `<button class="logo" data-action="navigate" data-view="home" aria-label="Ir para o início"><img class="logo-mark" src="assets/stowup-symbol.png" alt="" /><span class="logo-name">Stow<i>Up</i></span></button>`;

async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(path, { ...options, headers });
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.message || "Não foi possível concluir a operação.");
  return data;
}

async function bootstrapApi() {
  try {
    spaces = await api("/api/spaces");
    if (state.token) {
      state.bookings = await api("/api/bookings");
      if (["OWNER", "ADMIN"].includes(state.user?.role)) state.userSpaces = (await api("/api/owner/dashboard")).spaces;
    }
    state.apiOnline = true;
    store.set("bookings", state.bookings);
    render();
  } catch (error) {
    state.apiOnline = false;
    render();
    toast("Backend indisponível — execute o Spring Boot pelo IntelliJ");
  }
}

function header() {
  const nav = [
    ["home", "Descobrir"], ["favorites", "Favoritos"], ["bookings", "Reservas"], ["owner", "Para proprietários"]
  ];
  return `
    <div class="topbar"><strong>Proteção StowUp:</strong> proprietários verificados e pagamentos protegidos</div>
    <header class="header">
      <div class="container header-inner">
        ${logo()}
        <nav class="desktop-nav" aria-label="Navegação principal">
          ${nav.map(([view, label]) => `<button class="nav-btn ${state.view === view ? "active" : ""}" data-action="navigate" data-view="${view}">${label}</button>`).join("")}
        </nav>
        <div class="header-actions">
          <button class="host-btn" data-action="navigate" data-view="owner">+ Anunciar espaço</button>
          <span class="api-indicator ${state.apiOnline ? "online" : ""}">${state.apiOnline ? "API online" : "modo local"}</span>
          <button class="profile-btn" data-action="login" aria-label="Abrir perfil">${state.user ? state.user.name.split(" ").map(x => x[0]).slice(0,2).join("") : "PH"}</button>
        </div>
      </div>
    </header>`;
}

function mobileNav() {
  const items = [["home", "⌂", "Início"], ["search", "⌕", "Buscar"], ["favorites", "♡", "Favoritos"], ["bookings", "▤", "Reservas"], ["owner", "□", "Anunciar"]];
  return `<nav class="mobile-nav">${items.map(([view, glyph, label]) => `<button class="${state.view === view || (view === "search" && state.view === "results") ? "active" : ""}" data-action="navigate" data-view="${view}"><b>${glyph}</b>${label}</button>`).join("")}</nav>`;
}

function footer() {
  return `<footer class="footer"><div class="container footer-inner">${logo()}<div>Espaço para o que importa.</div><div class="footer-links"><button data-action="toast" data-message="Central de ajuda em breve">Ajuda</button><button data-action="toast" data-message="Política de privacidade aberta">Privacidade</button></div></div></footer>`;
}

function homeView() {
  return `
    <section class="hero">
      <div class="container hero-grid">
        <div>
          <div class="eyebrow">Armazenamento simples e seguro</div>
          <h1>Mais espaço para <em>viver melhor.</em></h1>
          <p class="hero-copy">Encontre boxes, depósitos e espaços verificados perto de você. Reserve online, pague com segurança e guarde sem complicação.</p>
          <form class="search-box" id="hero-search">
            <div class="field"><label for="location">Onde você precisa?</label><input id="location" name="location" placeholder="Cidade, bairro ou CEP" value="São Paulo" required /></div>
            <div class="field"><label for="space-type">Tipo de espaço</label><select id="space-type" name="type"><option>Todos</option>${["Box", "Depósito", "Garagem", "Galpão", "Armário", "Container"].map(t => `<option>${t}</option>`).join("")}</select></div>
            <button class="primary-btn" type="submit">Buscar espaços</button>
          </form>
          <div class="trust-row"><span>✓ Sem taxa para pesquisar</span><span>✓ Pagamento protegido</span><span>✓ Contrato digital</span></div>
        </div>
        <div class="hero-art" aria-hidden="true">
          <div class="art-main"><div class="warehouse"><div class="door"></div><div class="box"></div><div class="box b2"></div></div></div>
          <div class="art-badge badge-one"><span class="badge-icon">✓</span><div><strong>100% verificado</strong><small>Espaços inspecionados</small></div></div>
          <div class="art-badge badge-two"><span class="badge-icon">★</span><div><strong>4,8 de avaliação</strong><small>por clientes StowUp</small></div></div>
        </div>
      </div>
    </section>
    <section class="section">
      <div class="container">
        <div class="section-head"><div><div class="eyebrow">Do tamanho da sua necessidade</div><h2>Que tipo de espaço você procura?</h2><p>De algumas caixas a um estoque inteiro.</p></div></div>
        <div class="category-grid">
          ${[["Box","▣","3–10 m²"],["Depósito","▤","8–25 m²"],["Garagem","▱","10–20 m²"],["Galpão","⌂","30+ m²"],["Armário","▥","1–4 m²"],["Container","▰","14–28 m²"]].map(([name, icon, range]) => `<button class="category" data-action="category" data-type="${name}"><div class="category-icon">${icon}</div><strong>${name}</strong><small>${range}</small></button>`).join("")}
        </div>
      </div>
    </section>
    <section class="section soft">
      <div class="container">
        <div class="section-head"><div><div class="eyebrow">Bem avaliados perto de você</div><h2>Espaços em destaque</h2><p>Opções verificadas em São Paulo.</p></div><button class="text-btn" data-action="navigate" data-view="search">Ver todos →</button></div>
        <div class="cards-grid">${spaces.slice(0, 3).map(spaceCard).join("")}</div>
      </div>
    </section>
    <section class="section">
      <div class="container">
        <div class="section-head"><div><div class="eyebrow">Sem burocracia</div><h2>Guardar ficou mais fácil</h2></div></div>
        <div class="steps">
          <div class="step"><div class="step-num">01</div><h3>Encontre</h3><p>Pesquise por localização, tamanho, preço e recursos de segurança.</p></div>
          <div class="step"><div class="step-num">02</div><h3>Reserve online</h3><p>Escolha o período, confira o valor total e pague em ambiente protegido.</p></div>
          <div class="step"><div class="step-num">03</div><h3>Guarde tranquilo</h3><p>Receba seu contrato digital e acompanhe toda a locação pela plataforma.</p></div>
        </div>
        <div class="host-banner"><div><div class="eyebrow" style="color:var(--lime)">Tem espaço sobrando?</div><h2>Transforme seu espaço ocioso em renda todo mês.</h2><p>Publique gratuitamente, receba solicitações e conte com pagamentos e contratos gerenciados pela StowUp.</p></div><button class="primary-btn lime" data-action="navigate" data-view="owner">Quero anunciar</button></div>
      </div>
    </section>`;
}

function spaceCard(space) {
  const favored = state.favorites.includes(space.id);
  return `<article class="space-card">
    <div class="card-visual" style="--tone-a:${space.toneA};--tone-b:${space.toneB}" data-action="open-space" data-id="${space.id}" role="button" tabindex="0">
      <span class="card-tag">${space.verified ? "✓ Verificado" : "Novo"}</span>
      <button class="heart ${favored ? "active" : ""}" data-action="toggle-favorite" data-id="${space.id}" aria-label="${favored ? "Remover dos" : "Adicionar aos"} favoritos">${favored ? "♥" : "♡"}</button>
    </div>
    <div class="card-body">
      <div class="card-row"><h3>${space.title}</h3><span class="rating"><b>★</b> ${space.rating}</span></div>
      <div class="location">${space.district}, ${space.city} · ${space.distance.toFixed(1).replace(".", ",")} km</div>
      <div class="feature-row"><span class="feature">${space.size} m²</span>${space.features.slice(0, 2).map(f => `<span class="feature">${f}</span>`).join("")}</div>
      <div class="price-row"><div class="price"><strong>${money(space.price)}</strong> <small>/ mês</small></div><button class="card-link" data-action="open-space" data-id="${space.id}">Ver espaço →</button></div>
    </div>
  </article>`;
}

function resultsView() {
  let list = [...spaces];
  if (state.query) list = list.filter(s => `${s.title} ${s.city} ${s.district} ${s.type}`.toLowerCase().includes(state.query.toLowerCase()) || state.query.toLowerCase().includes("são paulo"));
  if (state.type !== "Todos") list = list.filter(s => s.type === state.type);
  if (state.size === "small") list = list.filter(s => s.size <= 8);
  if (state.size === "medium") list = list.filter(s => s.size > 8 && s.size <= 18);
  if (state.size === "large") list = list.filter(s => s.size > 18);
  if (state.sort === "price") list.sort((a, b) => a.price - b.price);
  if (state.sort === "rating") list.sort((a, b) => b.rating - a.rating);
  return `<main class="page"><div class="container">
    <h1 class="page-title">Espaços em São Paulo</h1><p class="page-subtitle"><strong>${list.length} opções</strong> verificadas para você comparar.</p>
    <div class="toolbar">
      <input class="filter-input" id="result-query" value="${escapeHTML(state.query || "São Paulo")}" placeholder="Bairro, cidade ou CEP" />
      <select class="filter-input" id="result-type"><option>Todos</option>${["Box","Depósito","Garagem","Galpão","Armário","Container"].map(t => `<option ${state.type === t ? "selected" : ""}>${t}</option>`).join("")}</select>
      <select class="filter-input" id="result-size"><option value="Todos">Qualquer tamanho</option><option value="small" ${state.size === "small" ? "selected" : ""}>Até 8 m²</option><option value="medium" ${state.size === "medium" ? "selected" : ""}>9 a 18 m²</option><option value="large" ${state.size === "large" ? "selected" : ""}>Mais de 18 m²</option></select>
      <select class="filter-input" id="result-sort"><option value="relevance">Mais relevantes</option><option value="price" ${state.sort === "price" ? "selected" : ""}>Menor preço</option><option value="rating" ${state.sort === "rating" ? "selected" : ""}>Melhor avaliação</option></select>
    </div>
    <div class="results-layout"><div class="result-list">${list.length ? list.map(spaceCard).join("") : emptyState("Nenhum espaço encontrado", "Tente remover alguns filtros ou buscar outra região.")}</div>
      <aside class="side-panel"><div class="eyebrow" style="color:var(--lime)">Sua região</div><h3>Opções próximas e verificadas</h3><p>Os endereços exatos são liberados após a confirmação da reserva para proteger proprietários e clientes.</p><div class="mini-map"><i class="pin p1"></i><i class="pin p2"></i><i class="pin p3"></i></div></aside>
    </div>
  </div></main>`;
}

function favoritesView() {
  const list = spaces.filter(s => state.favorites.includes(s.id));
  return `<main class="page"><div class="container"><h1 class="page-title">Seus favoritos</h1><p class="page-subtitle">Compare os espaços que mais combinam com você.</p><div class="cards-grid">${list.length ? list.map(spaceCard).join("") : emptyState("Sua lista está vazia", "Toque no coração de um espaço para guardar aqui.", "Explorar espaços")}</div></div></main>`;
}

function bookingsView() {
  return `<main class="page"><div class="container"><h1 class="page-title">Minhas reservas</h1><p class="page-subtitle">Acompanhe pagamentos, contratos e o período de armazenamento.</p>
    ${state.bookings.length ? state.bookings.map(booking => {
      const s = spaces.find(x => x.id === booking.spaceId) || spaces[0];
      return `<article class="booking-card"><div class="thumb" style="--tone-a:${s.toneA};--tone-b:${s.toneB}"></div><div><div class="card-row"><h3 style="margin:0">${s.title}</h3><span class="status ${booking.status === "Cancelada" ? "cancelled" : ""}">${booking.status}</span></div><div class="booking-meta"><span>Reserva ${booking.id}</span><span>Início ${dateBR(booking.start)}</span><span>${booking.months} ${booking.months === 1 ? "mês" : "meses"}</span><span>${money(booking.total)}</span></div></div><button class="contract-link" data-action="contract" data-id="${booking.contract}">Ver contrato</button></article>`;
    }).join("") : emptyState("Nenhuma reserva ainda", "Quando você reservar um espaço, os detalhes aparecerão aqui.", "Encontrar um espaço")}
  </div></main>`;
}

function ownerView() {
  if (!state.user || !["OWNER", "ADMIN"].includes(state.user.role)) return `<main class="page"><div class="container">${emptyState("Área do proprietário", "Entre com uma conta de proprietário aprovada para cadastrar e administrar espaços.")}</div></main>`;
  const listings = state.userSpaces.length ? state.userSpaces : spaces.slice(0, 2);
  return `<main class="page"><div class="container">
    <div class="section-head"><div><div class="eyebrow">Painel do proprietário</div><h1 class="page-title">Olá, Pedro 👋</h1><p>Veja como seus espaços estão performando.</p></div><button class="primary-btn" data-action="add-space">+ Novo espaço</button></div>
    <div class="stats"><div class="stat"><span>Receita neste mês</span><strong>R$ 3.840</strong><small>↑ 12%</small></div><div class="stat"><span>Taxa de ocupação</span><strong>78%</strong><small>↑ 6%</small></div><div class="stat"><span>Reservas ativas</span><strong>7</strong></div><div class="stat"><span>Avaliação média</span><strong>4,9 ★</strong></div></div>
    <div class="dashboard-grid"><section class="panel"><div class="panel-head"><h2>Desempenho mensal</h2><select class="filter-input"><option>Últimos 6 meses</option></select></div><div class="chart">${[[42,"Fev"],[55,"Mar"],[48,"Abr"],[70,"Mai"],[64,"Jun"],[86,"Jul"]].map(([h,m],i) => `<div class="bar ${i === 5 ? "current" : ""}" style="--h:${h}%"><span>${m}</span></div>`).join("")}</div></section><section class="panel"><div class="panel-head"><h3>Próximas entradas</h3></div>${[["AM","Ana Martins","02 ago · Box 08"],["CS","Caio Souza","05 ago · Depósito 14"],["RL","Renata Lima","11 ago · Box 03"]].map(([a,n,d]) => `<div class="booking-item"><div class="avatar">${a}</div><div class="item-copy"><strong>${n}</strong><small>${d}</small></div><span class="status">Confirmada</span></div>`).join("")}</section></div>
    <section class="panel" style="margin-top:24px"><div class="panel-head"><h2>Meus espaços</h2><button class="text-btn" data-action="add-space">Adicionar novo →</button></div>${listings.map(s => `<div class="listing-item"><div class="thumb" style="--tone-a:${s.toneA};--tone-b:${s.toneB}"></div><div class="item-copy"><strong>${s.title}</strong><small>${s.district} · ${s.size} m² · ${money(s.price)}/mês</small></div><span class="status">Publicado</span><button class="ghost-btn" data-action="toast" data-message="Editor do anúncio aberto">Editar</button></div>`).join("")}</section>
  </div></main>`;
}

function emptyState(title, text, actionLabel) {
  return `<div class="empty-state"><div class="empty-icon">⌂</div><h2>${title}</h2><p>${text}</p>${actionLabel ? `<button class="primary-btn" data-action="navigate" data-view="search">${actionLabel}</button>` : ""}</div>`;
}

function render() {
  const views = { home: homeView, search: resultsView, results: resultsView, favorites: favoritesView, bookings: bookingsView, owner: ownerView };
  app.innerHTML = `<div class="shell">${header()}${(views[state.view] || homeView)()}${footer()}${mobileNav()}</div>`;
}

function openSpace(id) {
  const s = spaces.find(x => x.id === Number(id));
  if (!s) return;
  state.selectedSpace = s;
  modalRoot.innerHTML = `<div class="modal-backdrop" data-action="close-modal"><div class="modal large" role="dialog" aria-modal="true" aria-label="Detalhes do espaço"><div class="detail-grid"><div class="detail-visual" style="--tone-a:${s.toneA};--tone-b:${s.toneB}"><span class="card-tag" style="left:20px;top:20px">✓ Espaço verificado</span></div><div class="detail-content"><button class="close-btn" data-action="close-modal" style="float:right">×</button><div class="eyebrow">${s.type} · ${s.size} m²</div><h2>${s.title}</h2><p class="location">${s.district}, ${s.city} · endereço completo após a reserva</p><span class="rating"><b>★</b> ${s.rating} (${s.reviews} avaliações)</span><p style="margin-top:18px">${s.description}</p><div class="detail-features">${s.features.map(f => `<div class="detail-feature">✓ ${f}</div>`).join("")}</div><div class="owner-line"><div class="avatar">${s.owner.split(" ").map(x => x[0]).slice(0,2).join("")}</div><div><strong>${s.owner}</strong><small style="display:block;color:var(--muted)">${s.ownerSince}</small></div></div><div class="price-row" style="margin-bottom:16px"><div class="price"><strong>${money(s.price)}</strong> <small>/ mês</small></div><span class="status">Disponível agora</span></div><button class="primary-btn wide" data-action="open-booking" data-id="${s.id}">Solicitar reserva</button><p class="secure-note">🔒 Você não será cobrado antes de revisar os detalhes.</p></div></div></div></div>`;
}

function openBooking(id) {
  const s = spaces.find(x => x.id === Number(id));
  if (!s) return;
  const today = new Date();
  today.setDate(today.getDate() + 7);
  const minDate = today.toISOString().slice(0, 10);
  modalRoot.innerHTML = `<div class="modal-backdrop" data-action="close-modal"><div class="modal" role="dialog" aria-modal="true" aria-label="Solicitar reserva"><div class="modal-head"><h2>Solicitar reserva</h2><button class="close-btn" data-action="close-modal">×</button></div><div class="modal-body"><div class="stepper"><i class="done">1</i><span class="done"></span><i class="done">2</i><span></span><i>3</i></div><div class="booking-item" style="padding-top:0"><div class="thumb" style="--tone-a:${s.toneA};--tone-b:${s.toneB}"></div><div class="item-copy"><strong>${s.title}</strong><small>${s.district} · ${s.size} m²</small></div></div><form id="booking-form" data-space-id="${s.id}"><div class="form-grid"><div class="form-group"><label for="start-date">Data de início</label><input type="date" id="start-date" name="start" min="${minDate}" value="${minDate}" required /></div><div class="form-group"><label for="months">Período inicial</label><select id="months" name="months"><option value="1">1 mês</option><option value="3" selected>3 meses</option><option value="6">6 meses</option><option value="12">12 meses</option></select></div><div class="form-group full"><label for="payment">Forma de pagamento</label><select id="payment" name="payment"><option>PIX</option><option>Cartão de crédito •••• 4242</option></select></div></div><div class="summary-box" id="booking-summary">${bookingSummary(s, 3)}</div><div class="secure-note">🔒 Pagamento protegido. O valor só será confirmado após a aprovação da reserva pelo proprietário.</div><button class="primary-btn wide" type="submit">Confirmar solicitação</button></form></div></div></div>`;
}

function bookingSummary(space, months) {
  const subtotal = space.price * months;
  const fee = Math.round(subtotal * .08);
  return `<div class="summary-line"><span>${money(space.price)} × ${months} ${months === 1 ? "mês" : "meses"}</span><strong>${money(subtotal)}</strong></div><div class="summary-line"><span>Taxa de proteção StowUp</span><strong>${money(fee)}</strong></div><div class="summary-line total"><span>Total</span><strong>${money(subtotal + fee)}</strong></div>`;
}

async function confirmBooking(form) {
  const data = new FormData(form);
  const s = spaces.find(x => x.id === Number(form.dataset.spaceId));
  const months = Number(data.get("months"));
  const total = s.price * months + Math.round(s.price * months * .08);
  let booking;
  try {
    booking = await api("/api/bookings", { method: "POST", body: JSON.stringify({ spaceId: s.id, start: data.get("start"), months, paymentMethod: data.get("payment") }) });
    state.bookings.unshift(booking);
    store.set("bookings", state.bookings);
  } catch (error) {
    toast(error.message);
    return;
  }
  modalRoot.innerHTML = `<div class="modal-backdrop"><div class="modal" role="dialog" aria-modal="true"><div class="modal-body" style="text-align:center;padding:44px"><div class="empty-icon" style="background:var(--lime)">✓</div><div class="eyebrow">Reserva confirmada</div><h2>Seu espaço está garantido!</h2><p>O pagamento foi autorizado e o contrato digital <strong>${booking.contract}</strong> já está disponível.</p><div class="summary-box"><div class="summary-line"><span>Início</span><strong>${dateBR(booking.start)}</strong></div><div class="summary-line"><span>Período</span><strong>${months} ${months === 1 ? "mês" : "meses"}</strong></div><div class="summary-line total"><span>Total</span><strong>${money(total)}</strong></div></div><button class="primary-btn wide" data-action="go-bookings">Acompanhar reserva</button></div></div></div>`;
}

function loginModal() {
  modalRoot.innerHTML = `<div class="modal-backdrop" data-action="close-modal"><div class="modal"><div class="modal-head"><h2>Bem-vindo de volta</h2><button class="close-btn" data-action="close-modal">×</button></div><div class="modal-body"><img class="brand-splash-logo" src="assets/stowup-logo.png" alt="StowUp — Seu espaço, quando precisar" /><p>Entre para acessar suas reservas, favoritos e espaços.</p><form id="login-form"><div class="form-grid"><div class="form-group full"><label>E-mail</label><input name="email" type="email" autocomplete="username" required /></div><div class="form-group full"><label>Senha</label><input name="password" type="password" autocomplete="current-password" required minlength="8" /></div></div><button class="primary-btn wide" style="margin-top:20px">Entrar com segurança</button><p class="secure-note">Ao continuar, você concorda com os Termos de Uso e a Política de Privacidade.</p></form></div></div></div>`;
}

function addSpaceModal() {
  modalRoot.innerHTML = `<div class="modal-backdrop" data-action="close-modal"><div class="modal"><div class="modal-head"><h2>Anunciar novo espaço</h2><button class="close-btn" data-action="close-modal">×</button></div><div class="modal-body"><p>Cadastre as informações essenciais. O anúncio passará por moderação antes de ser publicado.</p><form id="space-form"><div class="form-grid"><div class="form-group full"><label>Título do anúncio</label><input name="title" placeholder="Ex.: Box seguro próximo ao metrô" required /></div><div class="form-group"><label>Tipo</label><select name="type">${["Box","Depósito","Garagem","Galpão","Armário","Container"].map(x => `<option>${x}</option>`).join("")}</select></div><div class="form-group"><label>Área disponível (m²)</label><input name="size" type="number" min="1" value="8" required /></div><div class="form-group"><label>Bairro</label><input name="district" placeholder="Vila Mariana" required /></div><div class="form-group"><label>Valor mensal (R$)</label><input name="price" type="number" min="50" value="350" required /></div><div class="form-group full"><label>Descrição</label><textarea name="description" placeholder="Conte sobre acesso, segurança e o que pode ser armazenado." required></textarea></div></div><button class="primary-btn wide" style="margin-top:20px">Enviar para análise</button></form></div></div></div>`;
}

async function saveSpace(form) {
  const d = Object.fromEntries(new FormData(form));
  try {
    const created = await api("/api/owner/spaces", { method: "POST", body: JSON.stringify({ title: d.title, city: "São Paulo", district: d.district, type: d.type, size: Number(d.size), price: Number(d.price), description: d.description, features: ["Aguardando validação", "Novo anúncio"] }) });
    state.userSpaces.push(created);
  } catch (error) {
    toast(error.message);
    return;
  }
  store.set("user-spaces", state.userSpaces);
  modalRoot.innerHTML = "";
  render();
  toast("Espaço enviado para análise com sucesso");
}

function contractModal(id) {
  modalRoot.innerHTML = `<div class="modal-backdrop" data-action="close-modal"><div class="modal"><div class="modal-head"><h2>Contrato digital ${id}</h2><button class="close-btn" data-action="close-modal">×</button></div><div class="modal-body"><div class="eyebrow">Documento assinado eletronicamente</div><h3>Contrato de intermediação e locação de espaço</h3><p>Este instrumento formaliza a reserva realizada entre cliente e proprietário por intermédio da plataforma StowUp.</p><div class="summary-box"><div class="summary-line"><span>Status</span><strong style="color:#287059">✓ Válido e assinado</strong></div><div class="summary-line"><span>Integridade</span><strong>Verificada</strong></div><div class="summary-line"><span>Versão</span><strong>1.0</strong></div></div><p class="secure-note">🔒 Este protótipo demonstra a consulta e rastreabilidade do contrato. A versão de produção integrará assinatura eletrônica e armazenamento seguro.</p><button class="primary-btn wide" data-action="toast" data-message="Cópia do contrato preparada">Baixar cópia em PDF</button></div></div></div>`;
}

function toast(message) {
  toastRoot.innerHTML = `<div class="toast">${escapeHTML(message)}</div>`;
  setTimeout(() => { toastRoot.innerHTML = ""; }, 2800);
}

document.addEventListener("click", event => {
  const target = event.target.closest("[data-action]");
  if (!target) return;
  const action = target.dataset.action;
  if (action === "navigate") {
    state.view = target.dataset.view === "search" ? "results" : target.dataset.view;
    render(); window.scrollTo({ top: 0, behavior: "smooth" });
  }
  if (action === "category") { state.type = target.dataset.type; state.query = "São Paulo"; state.view = "results"; render(); window.scrollTo(0,0); }
  if (action === "open-space") openSpace(target.dataset.id);
  if (action === "toggle-favorite") {
    event.stopPropagation();
    const id = Number(target.dataset.id);
    state.favorites = state.favorites.includes(id) ? state.favorites.filter(x => x !== id) : [...state.favorites, id];
    store.set("favorites", state.favorites); render(); toast(state.favorites.includes(id) ? "Adicionado aos favoritos" : "Removido dos favoritos");
  }
  if (action === "open-booking") openBooking(target.dataset.id);
  if (action === "close-modal") {
    if (target.classList.contains("modal-backdrop") && event.target !== target) return;
    modalRoot.innerHTML = "";
  }
  if (action === "go-bookings") { modalRoot.innerHTML = ""; state.view = "bookings"; render(); }
  if (action === "login") loginModal();
  if (action === "add-space") addSpaceModal();
  if (action === "contract") contractModal(target.dataset.id);
  if (action === "toast") toast(target.dataset.message);
  if (action === "approve") { const row = document.getElementById(target.dataset.target); if (row) { row.querySelector(".secondary-btn").replaceWith(Object.assign(document.createElement("span"), { className: "status", textContent: "Aprovado" })); toast("Proprietário aprovado e notificado"); } }
});

document.addEventListener("submit", async event => {
  event.preventDefault();
  if (event.target.id === "hero-search") {
    const d = new FormData(event.target); state.query = d.get("location"); state.type = d.get("type"); state.view = "results"; render(); window.scrollTo(0,0);
  }
  if (event.target.id === "booking-form") await confirmBooking(event.target);
  if (event.target.id === "login-form") {
    const credentials = Object.fromEntries(new FormData(event.target));
    try {
      const auth = await api("/api/auth/login", { method: "POST", body: JSON.stringify(credentials) });
      state.token = auth.token;
      state.user = auth.user;
      state.apiOnline = true;
      store.set("token", state.token);
      store.set("user", state.user);
      modalRoot.innerHTML = "";
      render();
      toast("Login realizado com sucesso");
    } catch (error) {
      toast(error.message);
    }
  }
  if (event.target.id === "space-form") await saveSpace(event.target);
});

document.addEventListener("change", event => {
  if (event.target.id === "result-type") { state.type = event.target.value; render(); }
  if (event.target.id === "result-size") { state.size = event.target.value; render(); }
  if (event.target.id === "result-sort") { state.sort = event.target.value; render(); }
  if (event.target.id === "months") { const s = spaces.find(x => x.id === Number(document.querySelector("#booking-form")?.dataset.spaceId)); if (s) document.querySelector("#booking-summary").innerHTML = bookingSummary(s, Number(event.target.value)); }
});

document.addEventListener("keydown", event => {
  if (event.key === "Escape") modalRoot.innerHTML = "";
  if (event.key === "Enter" && event.target.matches(".card-visual")) openSpace(event.target.dataset.id);
});

let queryTimer;
document.addEventListener("input", event => {
  if (event.target.id !== "result-query") return;
  clearTimeout(queryTimer);
  queryTimer = setTimeout(() => { state.query = event.target.value; render(); }, 300);
});

render();
bootstrapApi();

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => navigator.serviceWorker.register("/sw.js").catch(() => {}));
}
