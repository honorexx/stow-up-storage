package br.com.stowup.user;

import br.com.stowup.account.application.AccountService;
import br.com.stowup.media.ImageStorage;
import br.com.stowup.user.AuthController.UserView;
import java.io.IOException;
import java.security.Principal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile/avatar")
public class ProfileMediaController {
  private final AccountService accounts;
  private final ImageStorage images;

  public ProfileMediaController(AccountService accounts, ImageStorage images) {
    this.accounts = accounts;
    this.images = images;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public UserView upload(@RequestPart("file") MultipartFile file, Principal principal)
      throws IOException {
    return UserView.from(accounts.updatePhoto(principal.getName(), images.saveProfile(file)));
  }
}
