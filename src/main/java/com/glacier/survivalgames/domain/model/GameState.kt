package com.glacier.survivalgames.domain.model

enum class GameState {
    Lobby,
    PreGame,
    LiveGame,
    PreDeathmatch,
    Deathmatch,

    PreFFADeathmatch,
    FFADeathmatch,

    PreTournamentDeathmatch,
    TournamentDeathmatch,

    EndGame,
    Cleanup
}