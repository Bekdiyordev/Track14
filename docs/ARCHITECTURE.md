# Architecture

## Layers

    UI (Compose screens, ViewModels)       :app  feature/*
        |  uses
    Domain (models, rules, contracts)      :domain
        ^  implemented by
    Data (Firebase repositories)           :app  data/*

Dependencies point inwards: `:domain` knows nothing about Android or Firebase.
`:app` wires the data layer to the domain contracts with Hilt (`core/di`).

## Rules of the road

1. Business rules (scoring, streaks, daily check) live in `:domain` and are covered by JVM unit tests.
2. ViewModels expose one immutable `StateFlow<UiState>`; screens are stateless functions of that state.
3. Repositories return `Flow` / `DomainResult`; Firebase exceptions are mapped once in `data/FirebaseErrors.kt`.
4. The client never writes coins, wallets, tests, day statuses, campaigns or the catalog.
   Those are written by Cloud Functions; the client only requests actions.
5. Navigation uses plain string routes now; typed routes arrive with the first screen that needs arguments.
6. Features become their own Gradle modules only when build time or team size asks for it.

## Source layout (`:app`, package `uz.beko404.track14`)

    core/designsystem   theme and brand tokens
    core/di             Hilt modules (Firebase, repositories)
    core/security       App Check installer (debug and release variants)
    data/<area>         Firebase implementations of domain contracts
    feature/<name>      screen + ViewModel + UiState
    navigation          bottom bar and NavHost

## Data flow example (catalog)

`catalog/{appId}` (public projection written by a Function)
-> `FirestoreAppsRepository` (snapshot listener, mapper)
-> `AppsViewModel` (`StateFlow<AppsUiState>`)
-> `AppsScreen`.

## Untrusted input

Ids from navigation or deep links are checked with `AppIds.isValid` before any Firestore call,
so a crafted id like `../wallets/x` can never address another document.

## Backend collections

As in the plan, section 7, plus `catalog/{appId}`: a denormalised public view of active
campaigns (name, category, plan, rewardPerDay, joined, needed, shortDesc, rating, featuredUntil, status, createdAt) with no owner data.
