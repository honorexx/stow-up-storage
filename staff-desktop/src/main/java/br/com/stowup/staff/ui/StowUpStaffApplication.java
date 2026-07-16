package br.com.stowup.staff.ui;

import br.com.stowup.staff.adapter.out.http.HttpStaffGateway;
import br.com.stowup.staff.adapter.out.process.LocalBackendStarter;
import br.com.stowup.staff.application.StaffConsoleService;
import br.com.stowup.staff.domain.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class StowUpStaffApplication extends Application {
  private static final String API_URL =
      System.getProperty(
          "stowup.api",
          System.getenv().getOrDefault("STOWUP_API_URL", "http://localhost:8080/api"));
  private final StaffConsoleService service =
      new StaffConsoleService(new HttpStaffGateway(API_URL));
  private Stage stage;
  private StaffSession session;
  private final Label toast = new Label();
  private final List<Timeline> refreshTimers = new ArrayList<>();

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    stage = primaryStage;
    stage.setTitle("StowUp · Central interna");
    stage.setMinWidth(1050);
    stage.setMinHeight(700);
    showLogin();
    stage.show();
  }

  private void showLogin() {
    stopAutoRefresh();
    Label brand = new Label("StowUp");
    brand.getStyleClass().add("brand");
    Label eyebrow = new Label("CENTRAL INTERNA");
    eyebrow.getStyleClass().add("eyebrow");
    Label title = new Label("Gestão e operação");
    title.getStyleClass().add("login-title");
    Label description = new Label("Entre com a conta mestre ou com seu acesso de funcionário.");
    description.getStyleClass().add("muted");
    description.setWrapText(true);
    TextField email = new TextField();
    email.setPromptText("SU123456 ou seu e-mail");
    PasswordField password = new PasswordField();
    password.setPromptText("Senha");
    Button login = primary("Entrar na central");
    Label connectionStatus = new Label();
    connectionStatus.getStyleClass().add("muted");
    connectionStatus.setWrapText(true);
    VBox card =
        new VBox(
            12,
            eyebrow,
            brand,
            title,
            description,
            field("Código SU ou e-mail", email),
            field("Senha", password),
            connectionStatus,
            login);
    card.getStyleClass().add("login-card");
    card.setMaxWidth(430);
    StackPane root = new StackPane(card);
    root.getStyleClass().add("login-background");
    StackPane.setMargin(card, new Insets(30));
    login.setOnAction(
        e ->
            run(
                () -> service.login(email.getText(), password.getText()),
                result -> {
                  session = result;
                  showDashboard();
                },
                login));
    prepareLocalBackend(login, connectionStatus);
    password.setOnAction(e -> login.fire());
    setScene(root, 1100, 720);
  }

  private void prepareLocalBackend(Button login, Label status) {
    login.setDisable(true);
    status.setText("Verificando PostgreSQL e Spring Boot...");
    LocalBackendStarter.ensureRunning(API_URL)
        .whenComplete(
            (result, error) ->
                Platform.runLater(
                    () -> {
                      login.setDisable(false);
                      if (error != null) {
                        status.setText(
                            "Serviços locais indisponíveis. Execute INICIAR.bat. "
                                + userMessage(error));
                        return;
                      }
                      if (result == LocalBackendStarter.Result.NOT_APPLICABLE) {
                        status.setText("");
                      } else {
                        status.setText("PostgreSQL e Spring Boot conectados.");
                      }
                    }));
  }

  private void showDashboard() {
    VBox sidebar = new VBox(10);
    sidebar.getStyleClass().add("sidebar");
    sidebar.setPrefWidth(230);
    Label brand = new Label("StowUp");
    brand.getStyleClass().add("brand-light");
    Label center = new Label("CENTRAL INTERNA");
    center.getStyleClass().add("eyebrow-light");
    VBox identity = new VBox(2, new Label(session.name()), new Label(session.roleLabel()));
    identity.getStyleClass().add("identity");
    TabPane tabs = new TabPane();
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    if (session.isMaster()) tabs.getTabs().add(new Tab("Visão geral", adminDashboardView()));
    if (session.canSupport()) tabs.getTabs().add(new Tab("Atendimento", supportView()));
    if (session.canModerate()) tabs.getTabs().add(new Tab("Anúncios", moderationView()));
    if (session.canReviewDocuments())
      tabs.getTabs().add(new Tab("Documentação", listingDocumentsView()));
    if (session.canReviewContracts()) tabs.getTabs().add(new Tab("Contratos", contractsView()));
    if (session.isMaster()) {
      tabs.getTabs().add(new Tab("Usuários do app", appUsersView()));
      tabs.getTabs().add(new Tab("Todos os anúncios", managedSpacesView()));
      tabs.getTabs().add(new Tab("Equipe", teamView()));
      tabs.getTabs()
          .add(
              new Tab(
                  "Pagamentos",
                  emptyState(
                      "Operação financeira",
                      "Acompanhe pagamentos e repasses do Mercado Pago sem visualizar dados sensíveis de cartão.")));
    }
    Button logout = new Button("Sair");
    logout.getStyleClass().add("sidebar-button");
    logout.setOnAction(
        e -> {
          session = null;
          showLogin();
        });
    Region spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);
    sidebar.getChildren().addAll(center, brand, new Separator(), identity, spacer, logout);
    toast.getStyleClass().add("toast");
    toast.setVisible(false);
    StackPane content = new StackPane(tabs, toast);
    StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
    StackPane.setMargin(toast, new Insets(18));
    BorderPane root = new BorderPane(content, null, null, null, sidebar);
    setScene(root, 1280, 800);
  }

  private Node adminDashboardView() {
    Label title = new Label("Visão geral da operação");
    title.getStyleClass().add("page-title");
    Label subtitle =
        new Label(
            "Indicadores calculados diretamente a partir dos usuários, espaços, reservas e contratos da plataforma.");
    subtitle.getStyleClass().add("muted");
    GridPane grid = new GridPane();
    grid.setHgap(14);
    grid.setVgap(14);
    ColumnConstraints column = new ColumnConstraints();
    column.setPercentWidth(25);
    grid.getColumnConstraints().addAll(column, column, column, column);
    VBox root = new VBox(18, title, subtitle, grid);
    root.setPadding(new Insets(28));
    ScrollPane scroll = new ScrollPane(root);
    scroll.setFitToWidth(true);
    Consumer<AdminDashboard> render =
        dashboard -> {
          grid.getChildren().clear();
          addMetric(grid, 0, 0, "Usuários", dashboard.totalUsers(), "Cadastros totais");
          addMetric(grid, 1, 0, "Clientes", dashboard.customers(), "Contas de clientes");
          addMetric(
              grid,
              2,
              0,
              "Proprietários",
              dashboard.owners(),
              dashboard.verifiedOwners() + " verificados");
          addMetric(
              grid,
              3,
              0,
              "Espaços ativos",
              dashboard.activeSpaces(),
              dashboard.occupiedSpaces() + " ocupados");
          addMetric(grid, 0, 1, "Em análise", dashboard.pendingSpaces(), "Aguardando moderação");
          addMetric(
              grid, 1, 1, "Reservas", dashboard.ongoingBookings(), "Pendentes ou em andamento");
          addMetric(
              grid, 2, 1, "Contratos ativos", dashboard.activeContracts(), "Aprovados e vigentes");
          addMetric(
              grid,
              3,
              1,
              "Chamados pendentes",
              dashboard.pendingReports(),
              "Aguardando atendimento");
          String revenue =
              NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                  .format(dashboard.platformRevenue());
          VBox revenueCard =
              metricCard("Receita da plataforma", revenue, "Comissão acumulada estimada");
          grid.add(revenueCard, 0, 2, 2, 1);
          GridPane.setHgrow(revenueCard, Priority.ALWAYS);
        };
    Supplier<AdminDashboard> load = () -> service.dashboard(session);
    run(load, render, null);
    autoRefresh(load, render);
    return scroll;
  }

  private void addMetric(
      GridPane grid, int column, int row, String label, long value, String detail) {
    VBox card = metricCard(label, String.valueOf(value), detail);
    grid.add(card, column, row);
    GridPane.setHgrow(card, Priority.ALWAYS);
  }

  private VBox metricCard(String label, String value, String detail) {
    Label caption = new Label(label);
    caption.getStyleClass().add("metric-label");
    Label number = new Label(value);
    number.getStyleClass().add("metric-value");
    Label note = new Label(detail);
    note.getStyleClass().add("muted");
    note.setWrapText(true);
    VBox card = new VBox(7, caption, number, note);
    card.getStyleClass().add("metric-card");
    card.setMaxWidth(Double.MAX_VALUE);
    return card;
  }

  private Node supportView() {
    ListView<SupportTicket> list = new ListView<>();
    list.setPrefWidth(360);
    list.setCellFactory(
        v ->
            new ListCell<>() {
              @Override
              protected void updateItem(SupportTicket item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setGraphic(null);
                setWrapText(true);
              }
            });
    VBox messages = new VBox(10);
    ScrollPane scroll = new ScrollPane(messages);
    scroll.setFitToWidth(true);
    VBox.setVgrow(scroll, Priority.ALWAYS);
    Label title = new Label("Selecione um chamado");
    title.getStyleClass().add("page-title");
    Label meta = new Label("A fila reúne o histórico de atendimento do cliente.");
    meta.getStyleClass().add("muted");
    TextArea reply = new TextArea();
    reply.setPromptText(
        "Escreva uma resposta clara e não solicite senhas ou dados completos de cartão.");
    reply.setPrefRowCount(3);
    Button send = primary("Enviar resposta");
    Button attach = secondary("Anexar arquivo");
    Button close = secondary("Encerrar chamado");
    HBox actions = new HBox(10, attach, close, send);
    actions.setAlignment(Pos.CENTER_RIGHT);
    VBox detail = new VBox(12, title, meta, scroll, reply, actions);
    detail.setPadding(new Insets(24));
    detail.setDisable(true);
    HBox.setHgrow(detail, Priority.ALWAYS);
    Supplier<List<SupportTicket>> loadTickets = () -> service.tickets(session, "ALL");
    Consumer<List<SupportTicket>> showTickets =
        items -> {
          SupportTicket selected = list.getSelectionModel().getSelectedItem();
          list.setItems(FXCollections.observableArrayList(items));
          if (selected != null)
            items.stream()
                .filter(item -> item.id().equals(selected.id()))
                .findFirst()
                .ifPresent(item -> list.getSelectionModel().select(item));
        };
    Runnable refresh = () -> run(loadTickets, showTickets, null);
    refresh.run();
    autoRefreshAlways(loadTickets, showTickets, Duration.seconds(3));
    autoRefreshAlways(
        () -> {
          SupportTicket selected = list.getSelectionModel().getSelectedItem();
          return selected == null
              ? List.<SupportMessage>of()
              : service.messages(session, selected.id());
        },
        items -> {
          SupportTicket selected = list.getSelectionModel().getSelectedItem();
          if (selected != null) showMessages(selected, items, messages);
        },
        Duration.seconds(2));
    list.getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, old, ticket) -> {
              if (ticket == null) return;
              detail.setDisable(false);
              title.setText(ticket.ticketNumber() + " · " + ticket.subject());
              meta.setText(
                  ticket.customerName()
                      + " · "
                      + ticket.customerEmail()
                      + " · "
                      + ticket.category()
                      + " · "
                      + ticket.status());
              reply.setDisable(false);
              send.setDisable(false);
              attach.setDisable(false);
              close.setText(
                  "CLOSED".equals(ticket.status()) ? "Reabrir chamado" : "Encerrar chamado");
              loadMessages(ticket, messages);
            });
    send.setOnAction(
        e -> {
          SupportTicket ticket = list.getSelectionModel().getSelectedItem();
          if (ticket == null) return;
          run(
              () -> service.reply(session, ticket.id(), reply.getText()),
              ignored -> {
                reply.clear();
                loadMessages(ticket, messages);
                refresh.run();
              },
              send);
        });
    attach.setOnAction(
        event -> {
          SupportTicket ticket = list.getSelectionModel().getSelectedItem();
          if (ticket == null) return;
          javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
          chooser.setTitle("Anexar arquivo ao atendimento");
          chooser
              .getExtensionFilters()
              .add(
                  new javafx.stage.FileChooser.ExtensionFilter(
                      "Arquivos permitidos",
                      "*.pdf",
                      "*.jpg",
                      "*.jpeg",
                      "*.png",
                      "*.webp",
                      "*.txt",
                      "*.zip",
                      "*.doc",
                      "*.docx"));
          java.io.File file = chooser.showOpenDialog(stage);
          if (file == null) return;
          run(
              () -> service.uploadSupportAttachment(session, ticket.id(), file.toPath()),
              ignored -> {
                loadMessages(ticket, messages);
                refresh.run();
              },
              attach);
        });
    close.setOnAction(
        e -> {
          SupportTicket ticket = list.getSelectionModel().getSelectedItem();
          if (ticket == null) return;
          String next = "CLOSED".equals(ticket.status()) ? "OPEN" : "CLOSED";
          run(
              () -> {
                service.changeTicketStatus(session, ticket.id(), next);
                return true;
              },
              ignored -> {
                refresh.run();
                detail.setDisable(true);
                title.setText("Selecione um chamado");
                messages.getChildren().clear();
              },
              close);
        });
    SplitPane split = new SplitPane(list, detail);
    split.setDividerPositions(.32);
    return split;
  }

  private void loadMessages(SupportTicket ticket, VBox box) {
    run(
        () -> service.messages(session, ticket.id()),
        items -> showMessages(ticket, items, box),
        null);
  }

  private void showMessages(SupportTicket ticket, List<SupportMessage> items, VBox box) {
    box.getChildren().clear();
    for (SupportMessage item : items) {
      Label sender = new Label(item.senderName() + " · " + format(item.createdAt()));
      sender.getStyleClass().add("message-sender");
      Label body = new Label(item.body());
      body.setWrapText(true);
      VBox bubble = new VBox(4, sender, body);
      if (item.attachmentName() != null && !item.attachmentName().isBlank()) {
        Button open = secondary("Abrir anexo · " + item.attachmentName());
        open.setOnAction(
            event ->
                run(
                    () ->
                        openTemporary(
                            service.downloadSupportAttachment(session, ticket.id(), item.id()),
                            item.attachmentName()),
                    this::openLocalFile,
                    open));
        bubble.getChildren().add(open);
      }
      bubble
          .getStyleClass()
          .add("SUPPORT".equals(item.senderType()) ? "message-staff" : "message-customer");
      box.getChildren().add(bubble);
    }
  }

  private Node moderationView() {
    ListView<PendingSpace> list = new ListView<>();
    list.setPrefWidth(370);
    list.setCellFactory(
        v ->
            new ListCell<>() {
              @Override
              protected void updateItem(PendingSpace item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setWrapText(true);
              }
            });
    Label title = new Label("Selecione um anúncio");
    title.getStyleClass().add("page-title");
    Label owner = new Label();
    owner.getStyleClass().add("muted");
    Label details = new Label();
    details.setWrapText(true);
    Label description = new Label();
    description.setWrapText(true);
    HBox moderationPhotos = new HBox(10);
    ScrollPane moderationPhotoScroll = new ScrollPane(moderationPhotos);
    moderationPhotoScroll.setFitToHeight(true);
    moderationPhotoScroll.setPrefHeight(140);
    TextArea reason = new TextArea();
    reason.setPromptText("Motivo obrigatório para rejeição (mínimo 10 caracteres)");
    reason.setPrefRowCount(3);
    Button approve = primary("Aprovar anúncio");
    Button reject = new Button("Rejeitar e devolver ao anunciante");
    reject.getStyleClass().add("danger-button");
    HBox actions = new HBox(10, reject, approve);
    actions.setAlignment(Pos.CENTER_RIGHT);
    VBox detail =
        new VBox(
            14,
            title,
            owner,
            details,
            new Label("Fotos enviadas pelo anunciante"),
            moderationPhotoScroll,
            new Separator(),
            description,
            new Region(),
            reason,
            actions);
    VBox.setVgrow(detail.getChildren().get(7), Priority.ALWAYS);
    detail.setPadding(new Insets(26));
    detail.setDisable(true);
    HBox.setHgrow(detail, Priority.ALWAYS);
    Supplier<List<PendingSpace>> loadSpaces = () -> service.pendingSpaces(session);
    Consumer<List<PendingSpace>> showSpaces =
        items -> list.setItems(FXCollections.observableArrayList(items));
    Runnable refresh = () -> run(loadSpaces, showSpaces, null);
    refresh.run();
    autoRefresh(loadSpaces, showSpaces);
    list.getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, old, space) -> {
              if (space == null) return;
              detail.setDisable(false);
              title.setText(space.title());
              owner.setText(space.ownerName() + " · " + space.ownerEmail());
              details.setText(
                  space.type()
                      + " · "
                      + space.size()
                      + " m² · R$ "
                      + space.price()
                      + "/mês\n"
                      + space.city()
                      + " · "
                      + space.district());
              description.setText(space.description());
              moderationPhotos.getChildren().clear();
              if (space.photos() == null || space.photos().isEmpty()) {
                moderationPhotos.getChildren().add(new Label("Nenhuma foto anexada."));
              } else {
                space
                    .photos()
                    .forEach(
                        path -> {
                          ImageView image = new ImageView(new Image(mediaUrl(path), true));
                          image.setFitWidth(170);
                          image.setFitHeight(110);
                          image.setPreserveRatio(true);
                          moderationPhotos.getChildren().add(image);
                        });
              }
              reason.clear();
            });
    approve.setOnAction(
        e -> {
          PendingSpace space = list.getSelectionModel().getSelectedItem();
          if (space != null)
            run(
                () -> {
                  service.approveSpace(session, space.id());
                  return true;
                },
                ignored -> {
                  notifyUser("Anúncio aprovado e publicado.");
                  detail.setDisable(true);
                  refresh.run();
                },
                approve);
        });
    reject.setOnAction(
        e -> {
          PendingSpace space = list.getSelectionModel().getSelectedItem();
          if (space != null)
            run(
                () -> {
                  service.rejectSpace(session, space.id(), reason.getText());
                  return true;
                },
                ignored -> {
                  notifyUser("Anúncio rejeitado com orientação ao anunciante.");
                  detail.setDisable(true);
                  refresh.run();
                },
                reject);
        });
    SplitPane split = new SplitPane(list, detail);
    split.setDividerPositions(.33);
    return split;
  }

  private Node contractsView() {
    ListView<StaffContract> list = new ListView<>();
    list.setPrefWidth(380);
    list.setCellFactory(
        v ->
            new ListCell<>() {
              @Override
              protected void updateItem(StaffContract item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setWrapText(true);
              }
            });
    Label title = new Label("Selecione um contrato");
    title.getStyleClass().add("page-title");
    Label people = new Label();
    people.getStyleClass().add("muted");
    Label warning =
        new Label(
            "A validação do VALIDAR/ITI comprova autenticidade e integridade da assinatura, não o conteúdo do contrato.");
    warning.setWrapText(true);
    warning.getStyleClass().add("muted");
    CheckBox nameMatches = new CheckBox("O nome do assinante confere com o locatário cadastrado");
    TextField reference = new TextField();
    reference.setPromptText("Referência, protocolo ou resumo do relatório VALIDAR/ITI");
    TextArea notes = new TextArea();
    notes.setPromptText("Observações obrigatórias da análise");
    notes.setPrefRowCount(4);
    Button openPdf = secondary("Abrir PDF assinado");
    Button validator = secondary("Abrir VALIDAR/ITI");
    Button reject = new Button("Rejeitar documento");
    reject.getStyleClass().add("danger-button");
    Button approve = primary("Aprovar e liberar pagamento");
    HBox tools = new HBox(10, openPdf, validator);
    HBox actions = new HBox(10, reject, approve);
    actions.setAlignment(Pos.CENTER_RIGHT);
    VBox detail =
        new VBox(
            13,
            title,
            people,
            warning,
            tools,
            new Separator(),
            nameMatches,
            reference,
            notes,
            new Region(),
            actions);
    VBox.setVgrow(detail.getChildren().get(9), Priority.ALWAYS);
    detail.setPadding(new Insets(26));
    detail.setDisable(true);
    Supplier<List<StaffContract>> loadContracts =
        () -> service.contracts(session, "PENDING_REVIEW");
    Consumer<List<StaffContract>> showContracts =
        items -> list.setItems(FXCollections.observableArrayList(items));
    Runnable refresh = () -> run(loadContracts, showContracts, null);
    refresh.run();
    autoRefresh(loadContracts, showContracts);
    list.getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, old, contract) -> {
              if (contract == null) return;
              detail.setDisable(false);
              title.setText(contract.contractCode() + " · " + contract.spaceTitle());
              people.setText(
                  "Locatário: "
                      + contract.customerName()
                      + " · "
                      + contract.customerEmail()
                      + "\nAnunciante: "
                      + contract.ownerName());
              nameMatches.setSelected(false);
              reference.clear();
              notes.clear();
            });
    openPdf.setOnAction(
        e -> {
          StaffContract c = list.getSelectionModel().getSelectedItem();
          if (c != null)
            run(
                () -> service.signedContract(session, c.id()),
                bytes -> {
                  try {
                    java.nio.file.Path file =
                        java.nio.file.Files.createTempFile(
                            "stowup-" + c.contractCode() + "-", ".pdf");
                    java.nio.file.Files.write(file, bytes);
                    java.awt.Desktop.getDesktop().open(file.toFile());
                  } catch (Exception ex) {
                    alert("Não foi possível abrir o PDF: " + ex.getMessage());
                  }
                },
                openPdf);
        });
    validator.setOnAction(
        e -> {
          try {
            java.awt.Desktop.getDesktop()
                .browse(java.net.URI.create("https://validar.iti.gov.br/"));
          } catch (Exception ex) {
            alert("Não foi possível abrir o VALIDAR/ITI.");
          }
        });
    approve.setOnAction(
        e ->
            reviewSelected(
                list,
                detail,
                refresh,
                "VALID",
                nameMatches.isSelected(),
                reference.getText(),
                notes.getText(),
                approve));
    reject.setOnAction(
        e ->
            reviewSelected(
                list,
                detail,
                refresh,
                "INVALID",
                nameMatches.isSelected(),
                reference.getText(),
                notes.getText(),
                reject));
    SplitPane split = new SplitPane(list, detail);
    split.setDividerPositions(.34);
    return split;
  }

  private void reviewSelected(
      ListView<StaffContract> list,
      VBox detail,
      Runnable refresh,
      String result,
      boolean nameMatches,
      String reference,
      String notes,
      Button button) {
    StaffContract contract = list.getSelectionModel().getSelectedItem();
    if (contract == null) return;
    if ("VALID".equals(result) && !nameMatches) {
      alert("Para aprovar, confirme que o nome do assinante confere com o cadastro.");
      return;
    }
    run(
        () -> {
          service.reviewContract(session, contract.id(), result, nameMatches, reference, notes);
          return true;
        },
        ignored -> {
          notifyUser(
              "VALID".equals(result)
                  ? "Contrato aprovado. O cliente já pode fazer o pagamento."
                  : "Contrato rejeitado para correção e reenvio.");
          detail.setDisable(true);
          refresh.run();
        },
        button);
  }

  private Node listingDocumentsView() {
    ListView<ListingDocument> list = new ListView<>();
    list.setPrefWidth(390);
    list.setCellFactory(
        v ->
            new ListCell<>() {
              @Override
              protected void updateItem(ListingDocument item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setWrapText(true);
              }
            });
    Label title = new Label("Selecione um documento");
    title.getStyleClass().add("page-title");
    Label owner = new Label();
    owner.getStyleClass().add("muted");
    Label explanation =
        new Label(
            "Confira o termo assinado pelo anunciante e valide a assinatura no VALIDAR/ITI antes de liberar a análise final do anúncio.");
    explanation.getStyleClass().add("muted");
    explanation.setWrapText(true);
    CheckBox nameMatches = new CheckBox("O nome do assinante confere com o anunciante cadastrado");
    TextField reference = new TextField();
    reference.setPromptText("Referência ou resumo do relatório VALIDAR/ITI");
    TextArea notes = new TextArea();
    notes.setPromptText("Observações obrigatórias");
    notes.setPrefRowCount(4);
    Button open = secondary("Abrir PDF assinado");
    Button validator = secondary("Abrir VALIDAR/ITI");
    Button approve = primary("Aprovar documento");
    Button reject = new Button("Solicitar correção");
    reject.getStyleClass().add("danger-button");
    HBox tools = new HBox(10, open, validator);
    HBox actions = new HBox(10, reject, approve);
    actions.setAlignment(Pos.CENTER_RIGHT);
    VBox detail =
        new VBox(
            13,
            title,
            owner,
            explanation,
            tools,
            new Separator(),
            nameMatches,
            reference,
            notes,
            new Region(),
            actions);
    VBox.setVgrow(detail.getChildren().get(8), Priority.ALWAYS);
    detail.setPadding(new Insets(26));
    detail.setDisable(true);
    Supplier<List<ListingDocument>> loadDocuments = () -> service.listingDocuments(session);
    Consumer<List<ListingDocument>> showDocuments =
        items -> list.setItems(FXCollections.observableArrayList(items));
    Runnable refresh = () -> run(loadDocuments, showDocuments, null);
    refresh.run();
    autoRefresh(loadDocuments, showDocuments);
    list.getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, old, doc) -> {
              if (doc == null) return;
              detail.setDisable(false);
              title.setText(doc.documentCode() + " · " + doc.spaceTitle());
              owner.setText(doc.ownerName() + " · " + doc.ownerEmail());
              nameMatches.setSelected(false);
              reference.clear();
              notes.clear();
            });
    open.setOnAction(
        e -> {
          ListingDocument doc = list.getSelectionModel().getSelectedItem();
          if (doc == null) return;
          run(
              () -> service.signedListingDocument(session, doc.id()),
              bytes -> {
                try {
                  java.nio.file.Path file =
                      java.nio.file.Files.createTempFile(
                          "stowup-" + doc.documentCode() + "-", ".pdf");
                  java.nio.file.Files.write(file, bytes);
                  java.awt.Desktop.getDesktop().open(file.toFile());
                } catch (Exception ex) {
                  alert("Não foi possível abrir o PDF: " + ex.getMessage());
                }
              },
              open);
        });
    validator.setOnAction(
        e -> {
          try {
            java.awt.Desktop.getDesktop()
                .browse(java.net.URI.create("https://validar.iti.gov.br/"));
          } catch (Exception ex) {
            alert("Não foi possível abrir o VALIDAR/ITI.");
          }
        });
    approve.setOnAction(
        e ->
            reviewListingDocument(
                list,
                detail,
                refresh,
                true,
                nameMatches.isSelected(),
                reference.getText(),
                notes.getText(),
                approve));
    reject.setOnAction(
        e ->
            reviewListingDocument(
                list,
                detail,
                refresh,
                false,
                nameMatches.isSelected(),
                reference.getText(),
                notes.getText(),
                reject));
    SplitPane split = new SplitPane(list, detail);
    split.setDividerPositions(.34);
    return split;
  }

  private void reviewListingDocument(
      ListView<ListingDocument> list,
      VBox detail,
      Runnable refresh,
      boolean approved,
      boolean nameMatches,
      String reference,
      String notes,
      Button button) {
    ListingDocument doc = list.getSelectionModel().getSelectedItem();
    if (doc == null) return;
    run(
        () -> {
          service.reviewListingDocument(session, doc.id(), approved, nameMatches, reference, notes);
          return true;
        },
        ignored -> {
          notifyUser(
              approved
                  ? "Documento aprovado. O anúncio já pode seguir para moderação."
                  : "Correção solicitada ao anunciante.");
          detail.setDisable(true);
          refresh.run();
        },
        button);
  }

  private Node appUsersView() {
    ListView<AppUser> list = new ListView<>();
    list.setPrefWidth(390);
    list.setCellFactory(
        ignored ->
            new ListCell<>() {
              @Override
              protected void updateItem(AppUser item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setWrapText(true);
              }
            });
    TextField search = new TextField();
    search.setPromptText("Pesquisar nome, e-mail, cidade ou ID");
    ComboBox<String> roleFilter =
        new ComboBox<>(FXCollections.observableArrayList("Todos", "Clientes", "Proprietários"));
    roleFilter.getSelectionModel().selectFirst();
    ComboBox<String> statusFilter =
        new ComboBox<>(FXCollections.observableArrayList("Todos", "Ativos", "Bloqueados"));
    statusFilter.getSelectionModel().selectFirst();
    Button reload = secondary("Atualizar");
    HBox filters = new HBox(8, roleFilter, statusFilter, reload);
    Label heading = new Label("Usuários do aplicativo");
    heading.getStyleClass().add("page-title");
    Label help = new Label("Clientes e proprietários cadastrados no mobile e na aplicação web.");
    help.getStyleClass().add("muted");
    VBox left = new VBox(10, heading, help, search, filters, list);
    left.setPadding(new Insets(22));
    VBox.setVgrow(list, Priority.ALWAYS);

    ImageView photo = new ImageView();
    photo.setFitWidth(92);
    photo.setFitHeight(92);
    photo.setPreserveRatio(true);
    Label selectedTitle = new Label("Selecione um usuário");
    selectedTitle.getStyleClass().add("page-title");
    Label accountMeta = new Label();
    accountMeta.getStyleClass().add("muted");
    TextField name = new TextField();
    TextField email = new TextField();
    email.setDisable(true);
    TextField phone = new TextField();
    TextField city = new TextField();
    TextField state = new TextField();
    ComboBox<RoleOption> role =
        new ComboBox<>(
            FXCollections.observableArrayList(
                new RoleOption("CUSTOMER", "Cliente"), new RoleOption("OWNER", "Proprietário")));
    role.setMaxWidth(Double.MAX_VALUE);
    CheckBox enabled = new CheckBox("Conta ativa");
    CheckBox verified = new CheckBox("Proprietário verificado");
    Button save = primary("Salvar usuário");
    Button delete = new Button("Excluir conta");
    delete.getStyleClass().add("danger-button");
    HBox actions = new HBox(10, delete, save);
    actions.setAlignment(Pos.CENTER_RIGHT);
    VBox edit =
        new VBox(
            12,
            new HBox(16, photo, new VBox(5, selectedTitle, accountMeta)),
            new Separator(),
            field("Nome", name),
            field("E-mail de login", email),
            field("Telefone", phone),
            new HBox(10, field("Cidade", city), field("UF", state)),
            field("Perfil", role),
            enabled,
            verified,
            actions);
    edit.setPadding(new Insets(24));
    edit.setDisable(true);
    ScrollPane editScroll = new ScrollPane(edit);
    editScroll.setFitToWidth(true);

    AtomicReference<List<AppUser>> cache = new AtomicReference<>(List.of());
    Runnable applyFilter =
        () -> {
          String term = search.getText().trim().toLowerCase(Locale.ROOT);
          String selectedRole = roleFilter.getValue();
          String selectedStatus = statusFilter.getValue();
          AppUser selected = list.getSelectionModel().getSelectedItem();
          List<AppUser> filtered =
              cache.get().stream()
                  .filter(
                      user ->
                          term.isBlank()
                              || (user.id()
                                      + " "
                                      + user.name()
                                      + " "
                                      + user.email()
                                      + " "
                                      + nullSafe(user.city()))
                                  .toLowerCase(Locale.ROOT)
                                  .contains(term))
                  .filter(
                      user ->
                          "Todos".equals(selectedRole)
                              || ("Clientes".equals(selectedRole) && "CUSTOMER".equals(user.role()))
                              || ("Proprietários".equals(selectedRole)
                                  && "OWNER".equals(user.role())))
                  .filter(
                      user ->
                          "Todos".equals(selectedStatus)
                              || ("Ativos".equals(selectedStatus) && user.enabled())
                              || ("Bloqueados".equals(selectedStatus) && !user.enabled()))
                  .toList();
          list.setItems(FXCollections.observableArrayList(filtered));
          if (selected != null)
            filtered.stream()
                .filter(user -> user.id() == selected.id())
                .findFirst()
                .ifPresent(user -> list.getSelectionModel().select(user));
        };
    Consumer<List<AppUser>> render =
        users -> {
          cache.set(users);
          applyFilter.run();
        };
    Runnable refresh = () -> run(() -> service.appUsers(session), render, reload);
    search.textProperty().addListener((ignored, old, value) -> applyFilter.run());
    roleFilter.setOnAction(event -> applyFilter.run());
    statusFilter.setOnAction(event -> applyFilter.run());
    reload.setOnAction(event -> refresh.run());
    refresh.run();
    autoRefresh(() -> service.appUsers(session), render);

    list.getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (ignored, old, user) -> {
              edit.setDisable(user == null);
              if (user == null) return;
              selectedTitle.setText(user.name());
              accountMeta.setText(
                  "ID "
                      + user.id()
                      + " · "
                      + user.listings()
                      + " anúncio(s) · cadastro "
                      + format(user.createdAt()));
              name.setText(user.name());
              email.setText(user.email());
              phone.setText(nullSafe(user.phone()));
              city.setText(nullSafe(user.city()));
              state.setText(nullSafe(user.state()));
              role.getSelectionModel().select(roleOption(user.role(), role.getItems()));
              enabled.setSelected(user.enabled());
              verified.setSelected(user.ownerVerified());
              verified.setDisable(!"OWNER".equals(user.role()));
              photo.setImage(
                  user.photoUrl() == null || user.photoUrl().isBlank()
                      ? null
                      : new Image(mediaUrl(user.photoUrl()), true));
            });
    role.setOnAction(
        event -> {
          RoleOption selected = role.getValue();
          verified.setDisable(selected == null || !"OWNER".equals(selected.code()));
          if (verified.isDisabled()) verified.setSelected(false);
        });
    save.setOnAction(
        event -> {
          AppUser user = list.getSelectionModel().getSelectedItem();
          RoleOption selectedRole = role.getValue();
          if (user == null) return;
          run(
              () ->
                  service.updateAppUser(
                      session,
                      user.id(),
                      name.getText(),
                      phone.getText(),
                      city.getText(),
                      state.getText(),
                      selectedRole == null ? "" : selectedRole.code(),
                      enabled.isSelected(),
                      verified.isSelected()),
              updated -> {
                notifyUser("Usuário atualizado no aplicativo.");
                refresh.run();
              },
              save);
        });
    delete.setOnAction(
        event -> {
          AppUser user = list.getSelectionModel().getSelectedItem();
          if (user == null) return;
          Alert confirmation =
              new Alert(
                  Alert.AlertType.CONFIRMATION,
                  "Excluir e anonimizar a conta de "
                      + user.name()
                      + "? A operação será bloqueada se existirem reservas ativas.",
                  ButtonType.CANCEL,
                  ButtonType.OK);
          confirmation.setHeaderText("Confirmar exclusão da conta");
          confirmation
              .showAndWait()
              .filter(ButtonType.OK::equals)
              .ifPresent(
                  ignored ->
                      run(
                          () -> {
                            service.deleteAppUser(session, user.id());
                            return true;
                          },
                          result -> {
                            edit.setDisable(true);
                            notifyUser("Conta excluída e dados pessoais anonimizados.");
                            refresh.run();
                          },
                          delete));
        });
    SplitPane split = new SplitPane(left, editScroll);
    split.setDividerPositions(.38);
    return split;
  }

  private Node managedSpacesView() {
    ListView<ManagedSpace> list = new ListView<>();
    list.setPrefWidth(390);
    list.setCellFactory(
        ignored ->
            new ListCell<>() {
              @Override
              protected void updateItem(ManagedSpace item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setWrapText(true);
              }
            });
    TextField search = new TextField();
    search.setPromptText("Pesquisar anúncio, proprietário, cidade ou ID");
    ComboBox<String> statusFilter =
        new ComboBox<>(
            FXCollections.observableArrayList(
                "Todos", "Em análise", "Ativos", "Ocupados", "Suspensos", "Retirados"));
    statusFilter.getSelectionModel().selectFirst();
    Button reload = secondary("Atualizar");
    Label heading = new Label("Todos os anúncios");
    heading.getStyleClass().add("page-title");
    Label help = new Label("Pesquisa, fotos, documentos, cadastro e situação operacional.");
    help.getStyleClass().add("muted");
    VBox left = new VBox(10, heading, help, search, new HBox(8, statusFilter, reload), list);
    left.setPadding(new Insets(22));
    VBox.setVgrow(list, Priority.ALWAYS);

    Label selectedTitle = new Label("Selecione um anúncio");
    selectedTitle.getStyleClass().add("page-title");
    Label meta = new Label();
    meta.getStyleClass().add("muted");
    Label status = new Label();
    status.getStyleClass().add("eyebrow");
    HBox photos = new HBox(10);
    ScrollPane photoScroll = new ScrollPane(photos);
    photoScroll.setFitToHeight(true);
    photoScroll.setPrefHeight(135);
    TextField title = new TextField();
    TextField owner = new TextField();
    owner.setDisable(true);
    TextField type = new TextField();
    TextField size = new TextField();
    size.setDisable(true);
    TextField height = new TextField();
    TextField length = new TextField();
    TextField depth = new TextField();
    TextField volume = new TextField();
    volume.setDisable(true);
    TextField price = new TextField();
    TextField city = new TextField();
    TextField district = new TextField();
    TextField postalCode = new TextField();
    TextField address = new TextField();
    TextField number = new TextField();
    TextField complement = new TextField();
    TextField state = new TextField();
    TextArea description = new TextArea();
    description.setWrapText(true);
    description.setPrefRowCount(4);
    ListView<AdminSpaceDocument> documents = new ListView<>();
    documents.setPrefHeight(145);
    Button openDocument = secondary("Abrir documento selecionado");
    Button openOwnerDocument = secondary("Abrir termo do anunciante");
    Label ownerDocumentStatus = new Label();
    ownerDocumentStatus.getStyleClass().add("muted");
    Button approve = primary("Aprovar anúncio");
    Button reject = new Button("Reprovar");
    reject.getStyleClass().add("danger-button");
    Button save = primary("Salvar alterações");
    Button addPhoto = secondary("Adicionar foto");
    Button withdraw = new Button("Retirar anúncio");
    withdraw.getStyleClass().add("danger-button");
    HBox moderationActions = new HBox(10, reject, approve);
    HBox editActions = new HBox(10, withdraw, save);
    moderationActions.setAlignment(Pos.CENTER_RIGHT);
    editActions.setAlignment(Pos.CENTER_RIGHT);
    VBox detail =
        new VBox(
            12,
            selectedTitle,
            meta,
            status,
            new HBox(10, new Label("Fotos do espaço"), addPhoto),
            photoScroll,
            field("Título", title),
            field("Proprietário", owner),
            new HBox(10, field("Tipo", type), field("Área calculada (m²)", size)),
            new HBox(
                10,
                field("Altura (m)", height),
                field("Comprimento (m)", length),
                field("Profundidade (m)", depth),
                field("Volume (m³)", volume)),
            field("Preço mensal", price),
            new HBox(10, field("Cidade", city), field("Bairro", district), field("UF", state)),
            new HBox(
                10, field("CEP", postalCode), field("Endereço", address), field("Número", number)),
            field("Complemento", complement),
            field("Descrição", description),
            new Separator(),
            new Label("Documentos anexados ao espaço"),
            documents,
            openDocument,
            ownerDocumentStatus,
            openOwnerDocument,
            moderationActions,
            editActions);
    detail.setPadding(new Insets(24));
    detail.setDisable(true);
    ScrollPane detailScroll = new ScrollPane(detail);
    detailScroll.setFitToWidth(true);

    AtomicReference<List<ManagedSpace>> cache = new AtomicReference<>(List.of());
    Runnable applyFilter =
        () -> {
          String term = search.getText().trim().toLowerCase(Locale.ROOT);
          String selectedStatus = statusFilter.getValue();
          ManagedSpace selected = list.getSelectionModel().getSelectedItem();
          List<ManagedSpace> filtered =
              cache.get().stream()
                  .filter(
                      space ->
                          term.isBlank()
                              || (space.id()
                                      + " "
                                      + space.title()
                                      + " "
                                      + space.ownerName()
                                      + " "
                                      + space.ownerEmail()
                                      + " "
                                      + space.city()
                                      + " "
                                      + space.district())
                                  .toLowerCase(Locale.ROOT)
                                  .contains(term))
                  .filter(space -> matchesSpaceStatus(space, selectedStatus))
                  .toList();
          list.setItems(FXCollections.observableArrayList(filtered));
          if (selected != null)
            filtered.stream()
                .filter(space -> space.id() == selected.id())
                .findFirst()
                .ifPresent(space -> list.getSelectionModel().select(space));
        };
    Consumer<List<ManagedSpace>> render =
        spaces -> {
          cache.set(spaces);
          applyFilter.run();
        };
    Runnable refresh = () -> run(() -> service.managedSpaces(session), render, reload);
    search.textProperty().addListener((ignored, old, value) -> applyFilter.run());
    statusFilter.setOnAction(event -> applyFilter.run());
    reload.setOnAction(event -> refresh.run());
    refresh.run();
    autoRefresh(() -> service.managedSpaces(session), render);

    list.getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (ignored, old, space) -> {
              detail.setDisable(space == null);
              if (space == null) return;
              selectedTitle.setText("#" + space.id() + " · " + space.title());
              meta.setText(space.ownerName() + " · " + space.ownerEmail());
              status.setText(
                  space.lifecycleStatus()
                      + " · moderação "
                      + space.moderationStatus()
                      + (space.moderationReason() == null ? "" : " · " + space.moderationReason()));
              title.setText(space.title());
              owner.setText(space.ownerName() + " · " + space.ownerEmail());
              type.setText(space.type());
              size.setText(String.valueOf(space.size()));
              height.setText(space.heightMeters().toPlainString());
              length.setText(space.lengthMeters().toPlainString());
              depth.setText(space.depthMeters().toPlainString());
              volume.setText(space.volumeCubicMeters().toPlainString());
              price.setText(space.price().toPlainString());
              city.setText(space.city());
              district.setText(space.district());
              postalCode.setText(nullSafe(space.postalCode()));
              address.setText(nullSafe(space.addressLine()));
              number.setText(nullSafe(space.addressNumber()));
              complement.setText(nullSafe(space.addressComplement()));
              state.setText(nullSafe(space.state()));
              description.setText(space.description());
              photos.getChildren().clear();
              if (space.photos() == null || space.photos().isEmpty()) {
                photos.getChildren().add(new Label("Nenhuma foto anexada."));
              } else {
                space
                    .photos()
                    .forEach(
                        path -> {
                          ImageView image = new ImageView(new Image(mediaUrl(path), true));
                          image.setFitWidth(150);
                          image.setFitHeight(105);
                          image.setPreserveRatio(true);
                          photos.getChildren().add(image);
                        });
              }
              ownerDocumentStatus.setText(
                  space.listingDocumentStatus() == null
                      ? "Termo do anunciante: não gerado"
                      : "Termo do anunciante: " + space.listingDocumentStatus());
              openOwnerDocument.setDisable(
                  space.listingDocumentId() == null
                      || "GENERATED".equals(space.listingDocumentStatus()));
              documents.setItems(FXCollections.observableArrayList());
              run(
                  () -> service.spaceDocuments(session, space.id()),
                  docs -> documents.setItems(FXCollections.observableArrayList(docs)),
                  null);
            });
    save.setOnAction(
        event -> {
          ManagedSpace current = list.getSelectionModel().getSelectedItem();
          if (current == null) return;
          run(
              () ->
                  service.updateManagedSpace(
                      session,
                      new ManagedSpace(
                          current.id(),
                          title.getText(),
                          current.ownerName(),
                          current.ownerEmail(),
                          current.ownerId(),
                          city.getText(),
                          district.getText(),
                          postalCode.getText(),
                          address.getText(),
                          number.getText(),
                          complement.getText(),
                          state.getText(),
                          type.getText(),
                          Integer.parseInt(size.getText()),
                          new BigDecimal(height.getText().replace(',', '.')),
                          new BigDecimal(length.getText().replace(',', '.')),
                          new BigDecimal(depth.getText().replace(',', '.')),
                          current.volumeCubicMeters(),
                          current.capacityTotal(),
                          current.capacityUsed(),
                          new BigDecimal(price.getText().replace(',', '.')),
                          description.getText(),
                          current.available(),
                          current.verified(),
                          current.moderationStatus(),
                          current.lifecycleStatus(),
                          current.moderationReason(),
                          current.photos(),
                          current.documentCount(),
                          current.listingDocumentId(),
                          current.listingDocumentStatus(),
                          current.listingDocumentFilename())),
              updated -> {
                notifyUser("Anúncio atualizado no aplicativo.");
                refresh.run();
              },
              save);
        });
    addPhoto.setOnAction(
        event -> {
          ManagedSpace space = list.getSelectionModel().getSelectedItem();
          if (space == null) return;
          javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
          chooser.setTitle("Selecionar foto do espaço");
          chooser
              .getExtensionFilters()
              .add(
                  new javafx.stage.FileChooser.ExtensionFilter(
                      "Imagens", "*.jpg", "*.jpeg", "*.png", "*.webp"));
          java.io.File file = chooser.showOpenDialog(stage);
          if (file == null) return;
          run(
              () -> service.uploadManagedSpacePhoto(session, space.id(), file.toPath()),
              updated -> {
                notifyUser("Foto adicionada ao anúncio e ao aplicativo.");
                refresh.run();
              },
              addPhoto);
        });
    approve.setOnAction(
        event -> {
          ManagedSpace space = list.getSelectionModel().getSelectedItem();
          if (space == null) return;
          run(
              () -> {
                service.approveSpace(session, space.id());
                return true;
              },
              result -> {
                notifyUser("Anúncio aprovado.");
                refresh.run();
              },
              approve);
        });
    reject.setOnAction(
        event -> {
          ManagedSpace space = list.getSelectionModel().getSelectedItem();
          if (space == null) return;
          TextInputDialog reason = new TextInputDialog();
          reason.setHeaderText("Motivo da reprovação");
          reason.setContentText("Explique ao proprietário:");
          reason
              .showAndWait()
              .ifPresent(
                  value ->
                      run(
                          () -> {
                            service.rejectSpace(session, space.id(), value);
                            return true;
                          },
                          result -> {
                            notifyUser("Anúncio reprovado.");
                            refresh.run();
                          },
                          reject));
        });
    withdraw.setOnAction(
        event -> {
          ManagedSpace space = list.getSelectionModel().getSelectedItem();
          if (space == null) return;
          Alert confirmation =
              new Alert(
                  Alert.AlertType.CONFIRMATION,
                  "Retirar o anúncio “" + space.title() + "” do aplicativo?",
                  ButtonType.CANCEL,
                  ButtonType.OK);
          confirmation.setHeaderText("Confirmar retirada do anúncio");
          confirmation
              .showAndWait()
              .filter(ButtonType.OK::equals)
              .ifPresent(
                  ignored ->
                      run(
                          () -> {
                            service.withdrawManagedSpace(session, space.id());
                            return true;
                          },
                          result -> {
                            notifyUser("Anúncio retirado do aplicativo.");
                            refresh.run();
                          },
                          withdraw));
        });
    openDocument.setOnAction(
        event -> {
          AdminSpaceDocument document = documents.getSelectionModel().getSelectedItem();
          if (document == null) return;
          run(
              () ->
                  openTemporary(
                      service.downloadSpaceDocument(session, document.id()), document.name()),
              this::openLocalFile,
              openDocument);
        });
    openOwnerDocument.setOnAction(
        event -> {
          ManagedSpace space = list.getSelectionModel().getSelectedItem();
          if (space == null || space.listingDocumentId() == null) return;
          run(
              () ->
                  openTemporary(
                      service.signedListingDocument(session, space.listingDocumentId()),
                      nullSafe(space.listingDocumentFilename()).isBlank()
                          ? "termo-anunciante.pdf"
                          : space.listingDocumentFilename()),
              this::openLocalFile,
              openOwnerDocument);
        });
    SplitPane split = new SplitPane(left, detailScroll);
    split.setDividerPositions(.36);
    return split;
  }

  private Node teamView() {
    ListView<StaffUser> list = new ListView<>();
    list.setPrefWidth(360);
    list.setCellFactory(
        v ->
            new ListCell<>() {
              @Override
              protected void updateItem(StaffUser item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setWrapText(true);
              }
            });

    Label heading = new Label("Equipe e permissões");
    heading.getStyleClass().add("page-title");
    Label explanation =
        new Label(
            "Cada pessoa entra com o próprio usuário e vê somente a área correspondente à função cadastrada.");
    explanation.getStyleClass().add("muted");
    explanation.setWrapText(true);
    Button reload = secondary("Atualizar lista");
    VBox left = new VBox(12, heading, explanation, reload, list);
    left.setPadding(new Insets(24));
    VBox.setVgrow(list, Priority.ALWAYS);

    TextField createName = new TextField();
    createName.setPromptText("Nome completo");
    TextField createEmail = new TextField();
    createEmail.setPromptText("funcionario@stowup.com");
    PasswordField createPassword = new PasswordField();
    createPassword.setPromptText("Mínimo de 10 caracteres");
    ComboBox<RoleOption> createRole = new ComboBox<>(staffRoles());
    createRole.getSelectionModel().selectFirst();
    createRole.setMaxWidth(Double.MAX_VALUE);
    Label createHelp =
        new Label(
            "O código SU é criado automaticamente. Cada função recebe somente a sua própria fila de trabalho.");
    createHelp.getStyleClass().add("muted");
    createHelp.setWrapText(true);
    Button create = primary("Cadastrar funcionário");
    VBox createForm =
        new VBox(
            12,
            field("Nome", createName),
            field("Usuário (e-mail)", createEmail),
            field("Senha temporária", createPassword),
            field("Função", createRole),
            createHelp,
            create);
    createForm.setPadding(new Insets(22));

    Label editTitle = new Label("Selecione um funcionário");
    editTitle.getStyleClass().add("page-title");
    TextField editCode = new TextField();
    editCode.setDisable(true);
    TextField editName = new TextField();
    TextField editEmail = new TextField();
    editEmail.setDisable(true);
    ComboBox<RoleOption> editRole = new ComboBox<>(staffRoles());
    editRole.setMaxWidth(Double.MAX_VALUE);
    CheckBox enabled = new CheckBox("Acesso ativo");
    PasswordField newPassword = new PasswordField();
    newPassword.setPromptText("Nova senha temporária");
    Button save = primary("Salvar acesso");
    Button resetPassword = secondary("Redefinir senha");
    Button delete = new Button("Excluir usuário");
    delete.getStyleClass().add("danger-button");
    HBox editActions = new HBox(10, delete, resetPassword, save);
    editActions.setAlignment(Pos.CENTER_RIGHT);
    VBox editForm =
        new VBox(
            12,
            editTitle,
            field("Código de acesso", editCode),
            field("Nome", editName),
            field("E-mail", editEmail),
            field("Função", editRole),
            enabled,
            new Separator(),
            field("Redefinir senha", newPassword),
            editActions);
    editForm.setPadding(new Insets(22));
    editForm.setDisable(true);

    TabPane forms =
        new TabPane(
            new Tab("Cadastrar funcionário", createForm), new Tab("Editar acesso", editForm));
    forms.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    HBox.setHgrow(forms, Priority.ALWAYS);

    Supplier<List<StaffUser>> loadStaff = () -> service.staffUsers(session);
    Consumer<List<StaffUser>> showStaff =
        users -> {
          if (editName.isFocused()
              || editRole.isFocused()
              || enabled.isFocused()
              || newPassword.isFocused()) return;
          StaffUser selected = list.getSelectionModel().getSelectedItem();
          list.setItems(FXCollections.observableArrayList(users));
          if (selected != null)
            users.stream()
                .filter(item -> item.id() == selected.id())
                .findFirst()
                .ifPresent(item -> list.getSelectionModel().select(item));
        };
    Runnable refresh = () -> run(loadStaff, showStaff, reload);
    reload.setOnAction(e -> refresh.run());
    refresh.run();
    autoRefresh(loadStaff, showStaff);

    create.setOnAction(
        e -> {
          RoleOption role = createRole.getValue();
          run(
              () ->
                  service.createStaff(
                      session,
                      createName.getText(),
                      createEmail.getText(),
                      createPassword.getText(),
                      role == null ? "" : role.code()),
              staff -> {
                createName.clear();
                createEmail.clear();
                createPassword.clear();
                createRole.getSelectionModel().selectFirst();
                notifyUser(
                    "Usuário criado: "
                        + staff.staffCode()
                        + " · entregue o código e a senha ao funcionário.");
                refresh.run();
              },
              create);
        });

    list.getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, old, staff) -> {
              if (staff == null) {
                editForm.setDisable(true);
                return;
              }
              editTitle.setText(staff.name() + " · " + staff.roleLabel());
              editCode.setText(staff.staffCode());
              editName.setText(staff.name());
              editEmail.setText(staff.email());
              editRole.getSelectionModel().select(roleOption(staff.role()));
              enabled.setSelected(staff.enabled());
              newPassword.clear();
              editForm.setDisable(staff.isMaster());
              if (staff.isMaster()) editTitle.setText(staff.name() + " · conta mestre protegida");
            });

    save.setOnAction(
        e -> {
          StaffUser staff = list.getSelectionModel().getSelectedItem();
          RoleOption role = editRole.getValue();
          if (staff == null) return;
          run(
              () ->
                  service.updateStaff(
                      session,
                      staff.id(),
                      editName.getText(),
                      role == null ? "" : role.code(),
                      enabled.isSelected()),
              updated -> {
                notifyUser("Acesso de " + updated.name() + " atualizado.");
                refresh.run();
              },
              save);
        });
    resetPassword.setOnAction(
        e -> {
          StaffUser staff = list.getSelectionModel().getSelectedItem();
          if (staff == null) return;
          run(
              () -> {
                service.resetStaffPassword(session, staff.id(), newPassword.getText());
                return true;
              },
              ignored -> {
                newPassword.clear();
                notifyUser("Senha redefinida. O funcionário já pode usar a nova senha.");
              },
              resetPassword);
        });
    delete.setOnAction(
        e -> {
          StaffUser staff = list.getSelectionModel().getSelectedItem();
          if (staff == null || staff.isMaster()) return;
          Alert confirmation =
              new Alert(
                  Alert.AlertType.CONFIRMATION,
                  "Excluir o acesso de "
                      + staff.name()
                      + "? Essa pessoa será desconectada e não poderá mais entrar.",
                  ButtonType.CANCEL,
                  ButtonType.OK);
          confirmation.setTitle("Excluir usuário interno");
          confirmation.setHeaderText("Confirme a exclusão do acesso");
          confirmation
              .showAndWait()
              .filter(ButtonType.OK::equals)
              .ifPresent(
                  ignored ->
                      run(
                          () -> {
                            service.deleteStaff(session, staff.id());
                            return true;
                          },
                          result -> {
                            editForm.setDisable(true);
                            notifyUser("Usuário excluído e acesso revogado.");
                            refresh.run();
                          },
                          delete));
        });

    SplitPane split = new SplitPane(left, forms);
    split.setDividerPositions(.34);
    return split;
  }

  private javafx.collections.ObservableList<RoleOption> staffRoles() {
    return FXCollections.observableArrayList(
        new RoleOption("SUPPORT", "Atendimento e suporte"),
        new RoleOption("LISTING_ANALYST", "Análise de anúncios"),
        new RoleOption("DOCUMENT_ANALYST", "Documentação do anunciante"),
        new RoleOption("CONTRACT_ANALYST", "Contratos de locação"));
  }

  private RoleOption roleOption(String code) {
    return staffRoles().stream()
        .filter(option -> option.code().equals(code))
        .findFirst()
        .orElse(null);
  }

  private RoleOption roleOption(
      String code, javafx.collections.ObservableList<RoleOption> options) {
    return options.stream().filter(option -> option.code().equals(code)).findFirst().orElse(null);
  }

  private boolean matchesSpaceStatus(ManagedSpace space, String filter) {
    if (filter == null || "Todos".equals(filter)) return true;
    return switch (filter) {
      case "Em análise" -> "IN_REVIEW".equals(space.lifecycleStatus());
      case "Ativos" -> "ACTIVE".equals(space.lifecycleStatus());
      case "Ocupados" -> "OCCUPIED".equals(space.lifecycleStatus());
      case "Suspensos" ->
          "SUSPENDED".equals(space.lifecycleStatus())
              || "TEMP_UNAVAILABLE".equals(space.lifecycleStatus());
      case "Retirados" -> "WITHDRAWN".equals(space.lifecycleStatus());
      default -> true;
    };
  }

  private String mediaUrl(String path) {
    if (path == null || path.isBlank()) return "";
    if (path.startsWith("http://") || path.startsWith("https://")) return path;
    String origin = API_URL.replaceFirst("/api/?$", "");
    return origin + (path.startsWith("/") ? path : "/" + path);
  }

  private String nullSafe(String value) {
    return value == null ? "" : value;
  }

  private Path openTemporary(byte[] bytes, String filename) {
    try {
      String safe =
          filename == null ? "documento.bin" : filename.replaceAll("[^A-Za-z0-9._-]", "_");
      int dot = safe.lastIndexOf('.');
      String suffix = dot >= 0 ? safe.substring(dot) : ".bin";
      Path file = Files.createTempFile("stowup-admin-", suffix);
      Files.write(file, bytes);
      file.toFile().deleteOnExit();
      return file;
    } catch (Exception exception) {
      throw new IllegalStateException(
          "Não foi possível preparar o documento para abertura.", exception);
    }
  }

  private void openLocalFile(Path file) {
    try {
      java.awt.Desktop.getDesktop().open(file.toFile());
    } catch (Exception exception) {
      alert("Não foi possível abrir o arquivo neste computador.");
    }
  }

  private record RoleOption(String code, String label) {
    @Override
    public String toString() {
      return label;
    }
  }

  private Node emptyState(String title, String message) {
    Label icon = new Label("SU");
    icon.getStyleClass().add("empty-icon");
    Label heading = new Label(title);
    heading.getStyleClass().add("page-title");
    Label body = new Label(message);
    body.getStyleClass().add("muted");
    body.setWrapText(true);
    VBox box = new VBox(12, icon, heading, body);
    box.setAlignment(Pos.CENTER);
    box.setMaxWidth(520);
    return new StackPane(box);
  }

  private VBox field(String label, Control control) {
    Label text = new Label(label);
    text.getStyleClass().add("field-label");
    return new VBox(6, text, control);
  }

  private Button primary(String text) {
    Button button = new Button(text);
    button.getStyleClass().add("primary-button");
    return button;
  }

  private Button secondary(String text) {
    Button button = new Button(text);
    button.getStyleClass().add("secondary-button");
    return button;
  }

  private <T> void autoRefresh(Supplier<T> work, Consumer<T> success) {
    AtomicBoolean inFlight = new AtomicBoolean();
    Timeline timer =
        new Timeline(
            new KeyFrame(
                Duration.seconds(10),
                e -> {
                  if (isUserEditing()) return;
                  if (!inFlight.compareAndSet(false, true)) return;
                  CompletableFuture.supplyAsync(work)
                      .whenComplete(
                          (result, error) ->
                              Platform.runLater(
                                  () -> {
                                    inFlight.set(false);
                                    if (error == null && session != null && !isUserEditing())
                                      success.accept(result);
                                  }));
                }));
    timer.setCycleCount(Timeline.INDEFINITE);
    timer.play();
    refreshTimers.add(timer);
  }

  private <T> void autoRefreshAlways(Supplier<T> work, Consumer<T> success, Duration interval) {
    AtomicBoolean inFlight = new AtomicBoolean();
    Timeline timer =
        new Timeline(
            new KeyFrame(
                interval,
                event -> {
                  if (!inFlight.compareAndSet(false, true)) return;
                  CompletableFuture.supplyAsync(work)
                      .whenComplete(
                          (result, error) ->
                              Platform.runLater(
                                  () -> {
                                    inFlight.set(false);
                                    if (error == null && session != null) success.accept(result);
                                  }));
                }));
    timer.setCycleCount(Timeline.INDEFINITE);
    timer.play();
    refreshTimers.add(timer);
  }

  private boolean isUserEditing() {
    if (stage == null || stage.getScene() == null) return false;
    Node focused = stage.getScene().getFocusOwner();
    return focused instanceof TextInputControl || focused instanceof ComboBoxBase<?>;
  }

  private void stopAutoRefresh() {
    refreshTimers.forEach(Timeline::stop);
    refreshTimers.clear();
  }

  private void setScene(Pane root, double width, double height) {
    Scene scene = new Scene(root, width, height);
    scene.getStylesheets().add(getClass().getResource("/stowup-staff.css").toExternalForm());
    stage.setScene(scene);
    stage.centerOnScreen();
  }

  private String format(java.time.Instant value) {
    return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(value);
  }

  private void notifyUser(String message) {
    toast.setText(message);
    toast.setVisible(true);
    CompletableFuture.delayedExecutor(4, java.util.concurrent.TimeUnit.SECONDS)
        .execute(() -> Platform.runLater(() -> toast.setVisible(false)));
  }

  private <T> void run(Supplier<T> work, java.util.function.Consumer<T> success, Button button) {
    if (button != null) button.setDisable(true);
    CompletableFuture.supplyAsync(work)
        .whenComplete(
            (result, error) ->
                Platform.runLater(
                    () -> {
                      if (button != null) button.setDisable(false);
                      if (error != null) alert(userMessage(error));
                      else success.accept(result);
                    }));
  }

  private void alert(String message) {
    Alert alert =
        new Alert(
            Alert.AlertType.ERROR,
            message == null ? "A operação não pôde ser concluída." : message,
            ButtonType.OK);
    alert.setHeaderText("Atenção");
    alert.showAndWait();
  }

  private String userMessage(Throwable value) {
    while ((value instanceof java.util.concurrent.CompletionException
            || value instanceof java.util.concurrent.ExecutionException)
        && value.getCause() != null) value = value.getCause();
    return value.getMessage();
  }
}
