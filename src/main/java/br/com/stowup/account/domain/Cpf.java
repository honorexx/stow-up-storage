package br.com.stowup.account.domain;

public final class Cpf {
  private Cpf() {}

  public static String normalize(String value) {
    String cpf = value == null ? "" : value.replaceAll("\\D", "");
    if (cpf.length() != 11
        || cpf.chars().distinct().count() == 1
        || digit(cpf, 9) != cpf.charAt(9) - '0'
        || digit(cpf, 10) != cpf.charAt(10) - '0') {
      throw new IllegalArgumentException("CPF inválido.");
    }
    return cpf;
  }

  private static int digit(String cpf, int length) {
    int sum = 0;
    for (int i = 0; i < length; i++) sum += (cpf.charAt(i) - '0') * (length + 1 - i);
    int result = 11 - (sum % 11);
    return result >= 10 ? 0 : result;
  }
}
