package br.com.stowup.space;

import jakarta.persistence.*;

@Entity
@Table(name = "space_photos")
public class SpacePhoto {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "space_id", nullable = false)
  private Space space;

  @Column(nullable = false, length = 300)
  private String url;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  protected SpacePhoto() {}

  public SpacePhoto(Space space, String url, int displayOrder) {
    this.space = space;
    this.url = url;
    this.displayOrder = displayOrder;
  }

  public String getUrl() {
    return url;
  }
}
