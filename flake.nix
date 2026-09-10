{
  description = "CPEN 321 Milestone 1";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    nixpkgs,
    flake-utils,
    ...
  }: let
    fl = flake-utils.lib;
  in
    fl.eachDefaultSystem (system: let
      pkgs = import nixpkgs {
        inherit system;

        config = {
          allowUnfree = true;
        };
      };
    in {
      devShells.default = pkgs.mkShell {
        packages = with pkgs; [
          # TS
          nodejs
          typescript
          typescript-language-server
          # Kotlin
          openjdk17
          kotlin
          gradle
          kotlin-language-server
          # Docker
          docker
        ];
      };
    });
}
