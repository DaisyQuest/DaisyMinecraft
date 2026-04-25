# DaisyMinecraft Admin Portal UX Hardening Plan

1. Establish a distinct DaisyMinecraft visual identity for the admin portal.
2. Replace flat page chrome with a layered dashboard composition.
3. Add a skip link for keyboard users.
4. Add sticky section navigation for long operator pages.
5. Keep the game endpoint visible in the hero.
6. Keep runtime reachability visible above the fold.
7. Show the Minecraft version above the fold.
8. Show server type above the fold.
9. Show selected content count above the fold.
10. Show backup health above the fold.
11. Distinguish runtime-ready, runtime-pending, and runtime-failed states.
12. Use a clear status pill with semantic colors.
13. Make failed state copy actionable.
14. Make pending state copy actionable.
15. Make reachable state copy precise about TCP versus protocol health.
16. Keep the primary action cluster near the runtime notice.
17. Separate destructive-looking actions from observational actions.
18. Add hover, focus, and busy states for controls.
19. Disable API buttons while a request is in flight.
20. Preserve action evidence in a dedicated live region.
21. Keep action logs readable on narrow screens.
22. Use responsive grids that collapse cleanly below tablet width.
23. Avoid horizontal page overflow at 375px width.
24. Make tables scroll within their panel instead of the viewport.
25. Add responsive labels to fact tables.
26. Improve card spacing with CSS variables.
27. Use stronger contrast for text on dark surfaces.
28. Use softer, production-grade panel shadows.
29. Use rounded containers consistently.
30. Add visual hierarchy between hero, summary, controls, and details.
31. Make the operator digest scannable.
32. Add a launch checklist beside technical details.
33. Add a health contract panel beside runtime details.
34. Show DaisyBase control-plane readiness.
35. Show marketplace policy readiness.
36. Show instance manager readiness.
37. Show whitelist state as a connection caveat.
38. Show online-mode state as a security caveat.
39. Keep RCON separate from game endpoint.
40. Use compact typography for long resource IDs.
41. Use monospaced styling for commands, paths, and logs.
42. Preserve all existing diagnostic data.
43. Preserve all existing admin actions.
44. Preserve all existing startup file visibility.
45. Preserve all existing provider evidence.
46. Add a refined runtime notice for reachable state.
47. Add a refined runtime notice for not-started state.
48. Add a refined runtime notice for failed state.
49. Add a stronger empty-state treatment.
50. Improve console tail readability.
51. Improve startup file readability.
52. Improve content lock readability.
53. Keep details expandable.
54. Add a two-column desktop layout for deep sections.
55. Stack deep sections on mobile.
56. Avoid fixed pixel widths that break small screens.
57. Use clamp-based typography.
58. Use viewport-aware page padding.
59. Use CSS-only decoration that does not block content.
60. Keep print and forced-color behavior acceptable.
61. Respect reduced-motion preferences.
62. Add keyboard-visible focus outlines.
63. Keep ARIA live output for action evidence.
64. Add explicit labels around action evidence.
65. Add section IDs for direct navigation.
66. Add semantic nav labels.
67. Keep headings in logical order.
68. Keep source order useful for screen readers.
69. Avoid color-only state communication.
70. Add text labels to every status indicator.
71. Make critical warnings visually dominant.
72. Make success states visually calm, not noisy.
73. Make warning states legible without alarm fatigue.
74. Ensure code blocks do not consume the full viewport on mobile.
75. Ensure details panels have touch-friendly summary targets.
76. Ensure buttons have touch-friendly hit targets.
77. Ensure main content remains readable at 320px width.
78. Ensure desktop view uses width without stretching text too far.
79. Ensure tablet view keeps primary controls visible.
80. Ensure mobile view keeps runtime status above controls.
81. Add Playwright viewport checks for desktop.
82. Add Playwright viewport checks for tablet.
83. Add Playwright viewport checks for mobile.
84. Capture screenshots for each viewport.
85. Check for horizontal overflow in Playwright.
86. Check for visible hero content in Playwright.
87. Check for visible controls in Playwright.
88. Check for visible runtime state in Playwright.
89. Check sticky navigation presence in Playwright.
90. Check fact table containment in Playwright.
91. Keep generated screenshots under `output/playwright`.
92. Keep Playwright verification repeatable without changing app state.
93. Run portal unit tests after markup changes.
94. Preserve existing HTTP tests.
95. Preserve existing runtime start and failure tests.
96. Avoid external font or asset dependencies.
97. Avoid adding frontend build tooling for this slice.
98. Keep the portal as server-rendered HTML.
99. Keep the UX pass reversible and localized.
100. Record verification output in the final delivery summary.
