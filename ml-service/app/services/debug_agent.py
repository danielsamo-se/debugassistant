"""
Debug agent using simple tool-calling loop with Gemini
"""

import os
import logging

from google import genai
from google.genai import types

from app.services.agent_tools import (
    analyze_stack_trace,
    search_similar_errors,
    get_framework_best_practices,
)


logger = logging.getLogger(__name__)


SYSTEM_PROMPT = """You are a debugging assistant that helps developers understand and fix errors in their code.

You have access to tools that help you analyze stack traces:
1. analyze_stack_trace - Extract structured info from the stack trace (use first)
2. search_similar_errors - Find similar errors in the knowledge base (use for solutions)
3. get_framework_best_practices - Get general guidance for a framework

Strategy:
1. First analyze the stack trace to identify the exception type and framework
2. Search for similar errors to find specific solutions
3. If needed, get framework best practices for additional context
4. Synthesize a clear explanation with actionable solutions

Be concise and practical. Focus on the root cause and concrete fixes."""


TOOL_MAP = {
    "analyze_stack_trace": analyze_stack_trace,
    "search_similar_errors": search_similar_errors,
    "get_framework_best_practices": get_framework_best_practices,
}

TOOL_DECLARATIONS = [
    {
        "name": "analyze_stack_trace",
        "description": "Extract structured info from a stack trace including exception type, framework, and root cause",
        "parameters": {
            "type": "object",
            "properties": {
                "stack_trace": {
                    "type": "string",
                    "description": "The stack trace to analyze"
                }
            },
            "required": ["stack_trace"]
        }
    },
    {
        "name": "search_similar_errors",
        "description": "Search the knowledge base for similar errors and their solutions",
        "parameters": {
            "type": "object",
            "properties": {
                "query": {
                    "type": "string",
                    "description": "The error message or exception to search for"
                }
            },
            "required": ["query"]
        }
    },
    {
        "name": "get_framework_best_practices",
        "description": "Get debugging best practices for a specific framework",
        "parameters": {
            "type": "object",
            "properties": {
                "framework": {
                    "type": "string",
                    "description": "The framework name (e.g. Spring Boot, Hibernate, React)"
                }
            },
            "required": ["framework"]
        }
    },
]


class DebugAgent:
    def __init__(self, api_key: str = None):
        self.api_key = api_key or os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY")
        self.client = None
        self.model = "gemini-2.5-flash-lite"

        if self.api_key:
            self.client = genai.Client(api_key=self.api_key)

    def analyze(self, stack_trace: str, max_iterations: int = 5) -> dict:
        if not self.client:
            logger.warning("Agent not initialized - missing GEMINI_API_KEY")
            return {"analysis": None, "tools_used": [], "error": "Agent not configured - set GEMINI_API_KEY"}

        tools_used = []
        contents = [
            types.Content(
                role="user",
                parts=[types.Part.from_text(text=
                                            f"Analyze this stack trace and provide a solution:\n\n{stack_trace}"
                                            )]
            )
        ]

        tool_config = types.Tool(function_declarations=[
            types.FunctionDeclaration(**t) for t in TOOL_DECLARATIONS
        ])

        try:
            for _ in range(max_iterations):
                response = self.client.models.generate_content(
                    model=self.model,
                    contents=contents,
                    config=types.GenerateContentConfig(
                        system_instruction=SYSTEM_PROMPT,
                        tools=[tool_config],
                        temperature=0,
                    )
                )

                candidate = response.candidates[0]

                # Check if model wants to call tools
                function_calls = [
                    p for p in candidate.content.parts
                    if p.function_call
                ]

                if not function_calls:
                    # No tool calls = final answer
                    final_text = response.text
                    return {
                        "analysis": final_text,
                        "tools_used": list(set(tools_used))
                    }

                # Execute each tool call
                contents.append(candidate.content)

                tool_response_parts = []
                for fc in function_calls:
                    fn_name = fc.function_call.name
                    fn_args = dict(fc.function_call.args)
                    tools_used.append(fn_name)

                    logger.info(f"Calling tool: {fn_name}({fn_args})")

                    if fn_name in TOOL_MAP:
                        result = TOOL_MAP[fn_name].invoke(fn_args)
                    else:
                        result = f"Unknown tool: {fn_name}"

                    tool_response_parts.append(
                        types.Part.from_function_response(
                            name=fn_name,
                            response={"result": str(result)}
                        )
                    )

                contents.append(
                    types.Content(role="user", parts=tool_response_parts)
                )

            return {
                "analysis": "Max iterations reached",
                "tools_used": list(set(tools_used))
            }

        except Exception as e:
            logger.error(f"Agent error: {e}")
            return {"analysis": None, "tools_used": [], "error": str(e)}


_debug_agent = None


def get_debug_agent() -> DebugAgent:
    global _debug_agent
    if _debug_agent is None:
        _debug_agent = DebugAgent()
    return _debug_agent