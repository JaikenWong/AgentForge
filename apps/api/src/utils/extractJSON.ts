export function extractJSON(text: string): string {
  // Try direct parse first
  try {
    JSON.parse(text);
    return text;
  } catch {}

  // Extract from ```json ... ``` blocks
  const jsonBlockMatch = text.match(/```(?:json)?\s*\n?([\s\S]*?)\n?```/);
  if (jsonBlockMatch) {
    return jsonBlockMatch[1].trim();
  }

  // Find first { ... } or [ ... ]
  const braceMatch = text.match(/(\{[\s\S]*\}|\[[\s\S]*\])/);
  if (braceMatch) {
    return braceMatch[1];
  }

  return text;
}