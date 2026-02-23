"""
Debug agent using LangGraph for autonomous tool selection
"""

import os
import logging
from typing import Annotated, TypedDict, Sequence

from langchain_core.messages import BaseMessage, HumanMessage, AIMessage, SystemMessage
from langchain_groq import ChatGroq
from langgraph.graph import StateGraph, END
from langgraph.prebuilt import ToolNode

from app.services.agent_tools import get_all_tools


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


class AgentState(TypedDict):
    messages: Annotated[Sequence[BaseMessage], "The messages in the conversation"]


class DebugAgent:
    def __init__(self, api_key: str = None):
        self.api_key = api_key or os.getenv("GROQ_API_KEY")
        self.tools = get_all_tools()
        self.graph = None

        if self.api_key:
            self._build_graph()

    def _build_graph(self):
        llm = ChatGroq(
            api_key=self.api_key,
            model="meta-llama/llama-4-scout-17b-16e-instruct",
            temperature=0
        )
        llm_with_tools = llm.bind_tools(self.tools)

        def should_continue(state: AgentState) -> str:
            last_message = state["messages"][-1]
            if hasattr(last_message, "tool_calls") and last_message.tool_calls:
                return "tools"
            return END

        def call_model(state: AgentState) -> dict:
            messages = state["messages"]
            response = llm_with_tools.invoke(messages)
            return {"messages": [response]}

        tool_node = ToolNode(self.tools)

        graph = StateGraph(AgentState)
        graph.add_node("agent", call_model)
        graph.add_node("tools", tool_node)
        graph.set_entry_point("agent")
        graph.add_conditional_edges("agent", should_continue, {"tools": "tools", END: END})
        graph.add_edge("tools", "agent")

        self.graph = graph.compile()

    def analyze(self, stack_trace: str) -> dict:
        if not self.graph:
            logger.warning("Agent not initialized - missing GROQ_API_KEY")
            return {"analysis": None, "tools_used": [], "error": "Agent not configured"}

        messages = [
            SystemMessage(content=SYSTEM_PROMPT),
            HumanMessage(content=f"Analyze this stack trace and provide a solution:\n\n{stack_trace}")
        ]

        try:
            result = self.graph.invoke({"messages": messages})

            tools_used = []
            final_response = None

            for msg in result["messages"]:
                if hasattr(msg, "tool_calls") and msg.tool_calls:
                    for tool_call in msg.tool_calls:
                        tools_used.append(tool_call["name"])
                if isinstance(msg, AIMessage) and msg.content and not getattr(msg, "tool_calls", None):
                    final_response = msg.content

            return {
                "analysis": final_response,
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