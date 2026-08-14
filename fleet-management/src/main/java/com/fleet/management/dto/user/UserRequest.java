package com.fleet.management.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato valido")
    @Size(max = 100, message = "El email no debe exceder los 100 caracteres")
    private String email;

    @NotBlank(message = "El password es obligatorio")
    @Size(min = 6, max = 255, message = "El password debe tener entre 6 y 255 caracteres")
    private String password;

    private Set<Long> roleIds;
}
