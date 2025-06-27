from dataclasses import dataclass
from typing import List, Dict, Any, Optional
from enum import Enum


@dataclass
class Parameter:
    name: str
    param_type: str
    required: bool = True
    description: str = ""


@dataclass
class FileInfo:
    path: str
    size: int
    file_type: str


@dataclass
class ProjectInfo:
    project_type: str
    root_path: str
    files: List[FileInfo]


@dataclass
class HTTPRequest:
    method: str
    path: str
    query_params: Dict[str, str]
    body: Dict[str, Any]

@dataclass
class APIInfo:
    controller_name: str 
    method_name: str 
    code_pos: str # /path/to/file.java:L1-L3
    http_method: str  # GET, POST, etc.
    req: HTTPRequest


@dataclass
class AnalysisResult:
    api_info: APIInfo
    analysis: str
    confidence: float
    suggestions: List[str]


@dataclass
class SummaryResult:
    total_apis: int
    api_summary: str
    recommendations: List[str]
    analysis_results: List[AnalysisResult]


@dataclass
class SymbolInfo:
    name: str
    symbol_type: str  # class, method, field, variable
    file_path: str
    line_number: int
    definition: str
    scope: str 